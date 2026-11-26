package com.example.service

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import coil.Coil
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.example.R
import com.example.audio.ZonexAudioEngine
import com.example.data.model.SoundItem
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object FloatingOverlayBus {
  private val _favoriteSounds = MutableStateFlow<List<SoundItem>>(emptyList())
  val favoriteSounds = _favoriteSounds.asStateFlow()

  private val _appLogoUrl = MutableStateFlow("")
  val appLogoUrl = _appLogoUrl.asStateFlow()

  private val _brandTitle = MutableStateFlow("ZONEX")
  val brandTitle = _brandTitle.asStateFlow()

  private val _isSystemOverlayRunning = MutableStateFlow(false)
  val isSystemOverlayRunning = _isSystemOverlayRunning.asStateFlow()

  private val _userClosedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 2)
  val userClosedEvents = _userClosedEvents.asSharedFlow()

  fun updateFavorites(items: List<SoundItem>) {
    _favoriteSounds.value = items
  }

  fun updateBranding(logoUrl: String, brandTitle: String) {
    _appLogoUrl.value = logoUrl
    _brandTitle.value = brandTitle.ifBlank { "ZONEX" }
  }

  fun setRunning(running: Boolean) {
    _isSystemOverlayRunning.value = running
  }

  fun notifyUserClosed() {
    _isSystemOverlayRunning.value = false
    _userClosedEvents.tryEmit(Unit)
  }
}

private class FloatingParticlesCanvasView(context: Context) : View(context) {
  private data class Particle(
    val baseX: Float,
    val baseY: Float,
    val radius: Float,
    val speedX: Float,
    val speedY: Float,
    val phase: Float,
    val color: Int,
  )

  private var phase = 0f
  private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
  private var animator: ValueAnimator? = null

  private val particles =
    listOf(
      Particle(0.18f, 0.20f, 18f, 0.05f, 0.08f, 0.1f, Color.parseColor("#38F5B82E")),
      Particle(0.78f, 0.14f, 24f, -0.04f, 0.06f, 1.3f, Color.parseColor("#3038BDF8")),
      Particle(0.52f, 0.45f, 28f, 0.03f, -0.05f, 2.4f, Color.parseColor("#2CA855F7")),
      Particle(0.24f, 0.72f, 20f, 0.06f, -0.07f, 3.6f, Color.parseColor("#35F5B82E")),
      Particle(0.84f, 0.78f, 22f, -0.05f, -0.04f, 4.9f, Color.parseColor("#2C22C55E")),
      Particle(0.45f, 0.88f, 14f, 0.07f, 0.05f, 5.2f, Color.parseColor("#3AFACC15")),
      Particle(0.12f, 0.50f, 10f, 0.04f, 0.06f, 0.9f, Color.parseColor("#44FFFFFF")),
      Particle(0.68f, 0.34f, 12f, -0.06f, 0.05f, 2.1f, Color.parseColor("#40F5B82E")),
      Particle(0.90f, 0.48f, 15f, -0.03f, -0.06f, 4.0f, Color.parseColor("#30EC4899")),
    )

  fun startAnim() {
    if (animator?.isRunning == true) return
    animator =
      ValueAnimator.ofFloat(0f, (2f * PI).toFloat()).apply {
        duration = 6500L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { anim ->
          phase = anim.animatedValue as Float
          invalidate()
        }
        start()
      }
  }

  fun stopAnim() {
    animator?.cancel()
    animator = null
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    if (w > 0 && h > 0) {
      bgPaint.shader =
        LinearGradient(
          0f,
          0f,
          w.toFloat(),
          h.toFloat(),
          intArrayOf(
            Color.parseColor("#F5090A12"),
            Color.parseColor("#F5141224"),
            Color.parseColor("#F50B111C"),
          ),
          floatArrayOf(0f, 0.55f, 1f),
          Shader.TileMode.CLAMP,
        )
    }
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    val w = width.toFloat()
    val h = height.toFloat()
    if (w <= 0f || h <= 0f) return
    canvas.drawRect(0f, 0f, w, h, bgPaint)

    val density = resources.displayMetrics.density
    particles.forEach { p ->
      val px = ((p.baseX + 0.08f * sin((phase + p.phase).toDouble())).toFloat().coerceIn(0.05f, 0.95f)) * w
      val py = ((p.baseY + 0.07f * cos((phase + p.phase * 1.3f).toDouble())).toFloat().coerceIn(0.05f, 0.95f)) * h
      glowPaint.color = p.color
      canvas.drawCircle(px, py, p.radius * density, glowPaint)
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val w =
      if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
        MeasureSpec.getSize(widthMeasureSpec)
      } else {
        0
      }
    val h =
      if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
        MeasureSpec.getSize(heightMeasureSpec)
      } else {
        0
      }
    setMeasuredDimension(w, h)
  }

  override fun onDetachedFromWindow() {
    stopAnim()
    super.onDetachedFromWindow()
  }
}

class FloatingSoundboardService : Service() {

  private data class FloatingCardHolder(
    val soundId: String,
    val rootCard: FrameLayout,
    val cardDrawable: GradientDrawable,
    val activeTintOverlay: View,
    val liveBadge: TextView,
    val equalizerRow: LinearLayout,
    val eqBars: List<View>,
    val playGlyph: TextView,
    val titleTxt: TextView,
    val bottomBar: LinearLayout,
  )

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var windowManager: WindowManager? = null
  private var windowParams: WindowManager.LayoutParams? = null
  private var rootContainer: FrameLayout? = null
  private var circleBubbleView: FrameLayout? = null
  private var bubbleBorderDrawable: GradientDrawable? = null
  private var bubbleLogoImageView: ImageView? = null
  private var expandedCardView: FrameLayout? = null
  private var particlesView: FloatingParticlesCanvasView? = null
  private var headerLogoImageView: ImageView? = null
  private var brandTextView: TextView? = null
  private var scrollContainer: ScrollView? = null
  private var gridContainer: GridLayout? = null

  private var isMinimized = true
  private var activePlayingSoundId: String? = null
  private val cardHolders = mutableListOf<FloatingCardHolder>()
  private val activeAnimators = mutableListOf<Animator>()
  private var bubblePulseAnimator: Animator? = null
  private val imageLoader by lazy { Coil.imageLoader(this) }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    val notification =
      ZonexNotificationHelper.buildControlNotification(
        this,
        "Zonex Sound Overlay",
        "Floating gaming soundboard active",
      )
    val fgResult =
      runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          startForeground(
            ZonexNotificationHelper.NOTIFICATION_ID_OVERLAY,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
          )
        } else {
          startForeground(ZonexNotificationHelper.NOTIFICATION_ID_OVERLAY, notification)
        }
      }
    if (fgResult.isFailure) {
      FloatingOverlayBus.setRunning(false)
      stopSelf()
      return
    }

    if (!Settings.canDrawOverlays(this)) {
      FloatingOverlayBus.setRunning(false)
      stopSelf()
      return
    }
    FloatingOverlayBus.setRunning(true)
    createOverlayWindow()
    observePlaybackState()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    updateHeaderBranding()
    populateFavoriteGrid()
    return START_NOT_STICKY
  }

  private fun observePlaybackState() {
    serviceScope.launch {
      ZonexAudioEngine.playbackState.collect { pb ->
        val nextId = if (pb.isPlaying || pb.isBuffering) pb.currentSound?.id else null
        if (nextId != activePlayingSoundId) {
          activePlayingSoundId = nextId
          refreshPlayingStateAnimations()
        }
      }
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  private fun createOverlayWindow() {
    windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

    val layoutFlag =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
      } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
      }

    // Start minimized as circular floating logo only
    isMinimized = true
    val params =
      WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        layoutFlag,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
      ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = dp(20)
        y = dp(120)
      }
    windowParams = params

    val rootWrapper = FrameLayout(this)
    val touchSlop = ViewConfiguration.get(this).scaledTouchSlop

    // 1. Circular Floating Logo Bubble (Initial & Minimized state)
    val bubbleDrawable =
      GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(Color.parseColor("#F00A0B0E"))
        setStroke(dp(2), Color.parseColor("#F5B82E"))
      }
    bubbleBorderDrawable = bubbleDrawable

    val bubbleSize = dp(60)
    val bubbleContainer =
      FrameLayout(this).apply {
        layoutParams = FrameLayout.LayoutParams(bubbleSize, bubbleSize)
        background = bubbleDrawable
        setPadding(dp(3), dp(3), dp(3), dp(3))
        elevation = dp(8).toFloat()
        clipToOutline = true
        outlineProvider =
          object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
              outline.setOval(0, 0, view.width, view.height)
            }
          }
      }

    val bubbleLogo =
      ImageView(this).apply {
        layoutParams =
          FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
          )
        scaleType = ImageView.ScaleType.CENTER_CROP
        clipToOutline = true
        outlineProvider =
          object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
              outline.setOval(0, 0, view.width, view.height)
            }
          }
        setImageResource(R.drawable.ic_floating_logo)
      }
    bubbleLogoImageView = bubbleLogo
    bubbleContainer.addView(bubbleLogo)

    var bubbleInitX = 0
    var bubbleInitY = 0
    var bubbleTouchX = 0f
    var bubbleTouchY = 0f
    var bubbleMoved = false

    bubbleContainer.setOnTouchListener { _, event ->
      when (event.action) {
        MotionEvent.ACTION_DOWN -> {
          bubbleInitX = params.x
          bubbleInitY = params.y
          bubbleTouchX = event.rawX
          bubbleTouchY = event.rawY
          bubbleMoved = false
          true
        }
        MotionEvent.ACTION_MOVE -> {
          val dx = event.rawX - bubbleTouchX
          val dy = event.rawY - bubbleTouchY
          if (hypot(dx.toDouble(), dy.toDouble()) > touchSlop) {
            bubbleMoved = true
          }
          params.x = bubbleInitX + dx.toInt()
          params.y = bubbleInitY + dy.toInt()
          runCatching { windowManager?.updateViewLayout(rootWrapper, params) }
          true
        }
        MotionEvent.ACTION_UP -> {
          if (!bubbleMoved) {
            setMinimizedState(false)
          }
          true
        }
        else -> false
      }
    }
    circleBubbleView = bubbleContainer

    // 2. Expanded Floating Window Card with Animated Floating Particles Backdrop
    val expandedCorner = dp(16).toFloat()
    val expandedShell =
      FrameLayout(this).apply {
        layoutParams = FrameLayout.LayoutParams(dp(236), FrameLayout.LayoutParams.WRAP_CONTENT)
        clipToOutline = true
        outlineProvider =
          object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
              outline.setRoundRect(0, 0, view.width, view.height, expandedCorner)
            }
          }
        foreground =
          GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            cornerRadius = expandedCorner
            setStroke(dp(1), Color.parseColor("#F5B82E"))
          }
        visibility = View.GONE
      }

    val particlesBg =
      FloatingParticlesCanvasView(this).apply {
        layoutParams =
          FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
          )
      }
    particlesView = particlesBg

    val expandedRoot =
      LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams =
          FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
          )
        setPadding(dp(8), dp(8), dp(8), dp(8))
      }

    // Header Row: [Logo] [ZONEX / GOLD] [■ STOP] [—] [×]
    val header =
      LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
      }

    val logoView =
      ImageView(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).apply { marginEnd = dp(6) }
        scaleType = ImageView.ScaleType.CENTER_CROP
        background =
          GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#181A22"))
            setStroke(dp(1), Color.parseColor("#F5B82E"))
          }
        setPadding(dp(2), dp(2), dp(2), dp(2))
        clipToOutline = true
        outlineProvider =
          object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
              outline.setOval(0, 0, view.width, view.height)
            }
          }
        setImageResource(R.drawable.ic_floating_logo)
      }
    headerLogoImageView = logoView

    val titleView =
      TextView(this).apply {
        text = "${FloatingOverlayBus.brandTitle.value}\nGOLD"
        setTextColor(Color.parseColor("#F5B82E"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        setLineSpacing(0f, 1.0f)
        typeface = Typeface.DEFAULT_BOLD
        maxLines = 2
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
      }
    brandTextView = titleView

    val stopBtn =
      TextView(this).apply {
        text = "■ STOP"
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setPadding(dp(8), dp(4), dp(8), dp(4))
        background =
          GradientDrawable().apply {
            setColor(Color.parseColor("#E11D48"))
            cornerRadius = dp(8).toFloat()
          }
        layoutParams =
          LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
          ).apply { marginEnd = dp(4) }
        setOnClickListener { ZonexAudioEngine.stopAll() }
      }

    val minBtn = createCircleIconBtn("—") { setMinimizedState(true) }
    val closeBtn =
      createCircleIconBtn("×") {
        FloatingOverlayBus.notifyUserClosed()
        stopSelf()
      }

    header.addView(logoView)
    header.addView(titleView)
    header.addView(stopBtn)
    header.addView(minBtn)
    header.addView(closeBtn)

    val grid =
      GridLayout(this).apply {
        columnCount = 2
        useDefaultMargins = false
      }
    gridContainer = grid

    val scroll =
      ScrollView(this).apply {
        isVerticalScrollBarEnabled = false
        layoutParams =
          LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(110),
          ).apply { topMargin = dp(8) }
        addView(grid)
      }
    scrollContainer = scroll

    expandedRoot.addView(header)
    expandedRoot.addView(scroll)
    expandedShell.addView(particlesBg)
    expandedShell.addView(expandedRoot)

    var initialX = 0
    var initialY = 0
    var initialTouchX = 0f
    var initialTouchY = 0f

    header.setOnTouchListener { _, event ->
      when (event.action) {
        MotionEvent.ACTION_DOWN -> {
          initialX = params.x
          initialY = params.y
          initialTouchX = event.rawX
          initialTouchY = event.rawY
          true
        }
        MotionEvent.ACTION_MOVE -> {
          params.x = initialX + (event.rawX - initialTouchX).toInt()
          params.y = initialY + (event.rawY - initialTouchY).toInt()
          runCatching { windowManager?.updateViewLayout(rootWrapper, params) }
          true
        }
        else -> false
      }
    }

    expandedCardView = expandedShell
    rootWrapper.addView(bubbleContainer)
    rootWrapper.addView(expandedShell)
    rootContainer = rootWrapper

    updateHeaderBranding()
    populateFavoriteGrid()
    runCatching {
      windowManager?.addView(rootWrapper, params)
    }.onFailure {
      rootContainer = null
      FloatingOverlayBus.setRunning(false)
      stopSelf()
    }
  }

  private fun setMinimizedState(minimized: Boolean) {
    isMinimized = minimized
    circleBubbleView?.visibility = if (minimized) View.VISIBLE else View.GONE
    expandedCardView?.visibility = if (minimized) View.GONE else View.VISIBLE
    if (minimized) {
      particlesView?.stopAnim()
    } else {
      particlesView?.startAnim()
    }
    val params = windowParams ?: return
    val root = rootContainer ?: return
    params.width = if (minimized) WindowManager.LayoutParams.WRAP_CONTENT else dp(236)
    runCatching { windowManager?.updateViewLayout(root, params) }
    refreshPlayingStateAnimations()
  }

  private fun updateHeaderBranding() {
    val logoUrl = FloatingOverlayBus.appLogoUrl.value
    val brand = FloatingOverlayBus.brandTitle.value
    brandTextView?.text = "$brand\nGOLD"
    val targets = listOfNotNull(headerLogoImageView, bubbleLogoImageView)
    if (logoUrl.isNotBlank()) {
      for (targetLogo in targets) {
        serviceScope.launch {
          val req =
            ImageRequest.Builder(this@FloatingSoundboardService)
              .data(logoUrl)
              .transformations(CircleCropTransformation())
              .target(targetLogo)
              .build()
          imageLoader.execute(req)
        }
      }
    } else {
      targets.forEach { it.setImageResource(R.drawable.ic_floating_logo) }
    }
  }

  private fun populateFavoriteGrid() {
    val grid = gridContainer ?: return
    cancelCardAnimators()
    cardHolders.clear()
    grid.removeAllViews()
    val items = FloatingOverlayBus.favoriteSounds.value
    if (items.isEmpty()) {
      scrollContainer?.layoutParams =
        LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT,
          LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(8) }
      val empty =
        TextView(this).apply {
          text = "হোমে গিয়ে ★ ট্যাপ করলে প্রিয় সাউন্ড এখানে পিন হবে"
          setTextColor(Color.parseColor("#CBD5E1"))
          setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
          setPadding(dp(6), dp(8), dp(6), dp(6))
        }
      grid.addView(empty)
      return
    }

    val scrollHeight = if (items.size <= 2) dp(106) else if (items.size <= 4) dp(212) else dp(264)
    scrollContainer?.layoutParams =
      LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        scrollHeight,
      ).apply { topMargin = dp(8) }

    val cornerPx = dp(10).toFloat()
    for (sound in items.take(16)) {
      val cardDrawable =
        GradientDrawable().apply {
          setColor(Color.parseColor("#B3161822"))
          cornerRadius = cornerPx
          setStroke(dp(1), Color.parseColor("#B8860B"))
        }

      val card =
        FrameLayout(this).apply {
          background = cardDrawable
          clipToOutline = true
          outlineProvider =
            object : ViewOutlineProvider() {
              override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, cornerPx)
              }
            }
          layoutParams =
            GridLayout.LayoutParams().apply {
              width = dp(102)
              height = dp(98)
              setMargins(dp(4), dp(4), dp(4), dp(4))
            }
          setOnClickListener {
            ZonexAudioEngine.toggleOrPlaySound(this@FloatingSoundboardService, sound)
          }
        }

      if (sound.thumbnailUrl.isNotBlank()) {
        val thumb =
          ImageView(this).apply {
            layoutParams =
              FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
              )
            scaleType = ImageView.ScaleType.CENTER_CROP
          }
        card.addView(thumb)
        serviceScope.launch {
          val req =
            ImageRequest.Builder(this@FloatingSoundboardService)
              .data(sound.thumbnailUrl)
              .target(thumb)
              .build()
          imageLoader.execute(req)
        }
      } else {
        val emojiView =
          TextView(this).apply {
            text = sound.emoji
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
            gravity = Gravity.CENTER
            layoutParams =
              FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
              )
          }
        card.addView(emojiView)
      }

      // Active gold highlight tint overlay
      val activeTint =
        View(this).apply {
          layoutParams =
            FrameLayout.LayoutParams(
              FrameLayout.LayoutParams.MATCH_PARENT,
              FrameLayout.LayoutParams.MATCH_PARENT,
            )
          setBackgroundColor(Color.parseColor("#2EF5B82E"))
          visibility = View.GONE
        }
      card.addView(activeTint)

      // Top-left gold star badge
      val badge =
        TextView(this).apply {
          text = "★"
          setTextColor(Color.parseColor("#F5B82E"))
          setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
          gravity = Gravity.CENTER
          background =
            GradientDrawable().apply {
              shape = GradientDrawable.OVAL
              setColor(Color.parseColor("#AA09090D"))
            }
          layoutParams =
            FrameLayout.LayoutParams(dp(18), dp(18), Gravity.TOP or Gravity.START).apply {
              setMargins(dp(5), dp(5), 0, 0)
            }
        }
      card.addView(badge)

      // Top-right LIVE badge when playing
      val liveBadge =
        TextView(this).apply {
          text = "LIVE"
          setTextColor(Color.BLACK)
          setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f)
          typeface = Typeface.DEFAULT_BOLD
          setPadding(dp(5), dp(1), dp(5), dp(1))
          background =
            GradientDrawable().apply {
              setColor(Color.parseColor("#F5B82E"))
              cornerRadius = dp(6).toFloat()
            }
          layoutParams =
            FrameLayout.LayoutParams(
              FrameLayout.LayoutParams.WRAP_CONTENT,
              FrameLayout.LayoutParams.WRAP_CONTENT,
              Gravity.TOP or Gravity.END,
            ).apply {
              setMargins(0, dp(5), dp(5), 0)
            }
          visibility = View.GONE
        }
      card.addView(liveBadge)

      // Bottom bar with Equalizer Strip + Play Glyph + Title
      val bottomContainer =
        LinearLayout(this).apply {
          orientation = LinearLayout.VERTICAL
          setBackgroundColor(Color.parseColor("#CC08090C"))
          setPadding(dp(6), dp(4), dp(6), dp(5))
          layoutParams =
            FrameLayout.LayoutParams(
              FrameLayout.LayoutParams.MATCH_PARENT,
              FrameLayout.LayoutParams.WRAP_CONTENT,
              Gravity.BOTTOM,
            )
        }

      val eqRow =
        LinearLayout(this).apply {
          orientation = LinearLayout.HORIZONTAL
          gravity = Gravity.BOTTOM
          layoutParams =
            LinearLayout.LayoutParams(
              LinearLayout.LayoutParams.MATCH_PARENT,
              dp(11),
            ).apply { bottomMargin = dp(3) }
          visibility = View.GONE
        }
      val bars =
        (0 until 6).map { idx ->
          View(this).apply {
            background =
              GradientDrawable().apply {
                setColor(Color.parseColor("#FFD54F"))
                cornerRadius = dp(2).toFloat()
              }
            layoutParams =
              LinearLayout.LayoutParams(0, dp(8), 1f).apply {
                if (idx < 5) marginEnd = dp(2)
              }
            pivotY = dp(8).toFloat()
          }.also { eqRow.addView(it) }
        }
      bottomContainer.addView(eqRow)

      val titleRow =
        LinearLayout(this).apply {
          orientation = LinearLayout.HORIZONTAL
          gravity = Gravity.CENTER_VERTICAL
        }
      val playGlyph =
        TextView(this).apply {
          text = "▶ "
          setTextColor(Color.parseColor("#F5B82E"))
          setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
        }
      val titleTxt =
        TextView(this).apply {
          text = sound.title
          setTextColor(Color.WHITE)
          setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
          typeface = Typeface.DEFAULT_BOLD
          maxLines = 1
          ellipsize = TextUtils.TruncateAt.END
        }
      titleRow.addView(playGlyph)
      titleRow.addView(titleTxt)
      bottomContainer.addView(titleRow)
      card.addView(bottomContainer)

      cardHolders.add(
        FloatingCardHolder(
          soundId = sound.id,
          rootCard = card,
          cardDrawable = cardDrawable,
          activeTintOverlay = activeTint,
          liveBadge = liveBadge,
          equalizerRow = eqRow,
          eqBars = bars,
          playGlyph = playGlyph,
          titleTxt = titleTxt,
          bottomBar = bottomContainer,
        )
      )
      grid.addView(card)
    }

    refreshPlayingStateAnimations()
  }

  private fun refreshPlayingStateAnimations() {
    cancelCardAnimators()
    val playingId = activePlayingSoundId

    // Pulse minimized bubble ring if something is playing while minimized
    bubblePulseAnimator?.cancel()
    bubblePulseAnimator = null
    val bubble = circleBubbleView
    if (bubble != null) {
      if (playingId != null && isMinimized) {
        bubbleBorderDrawable?.setStroke(dp(3), Color.parseColor("#FFD54F"))
        bubblePulseAnimator =
          ObjectAnimator.ofPropertyValuesHolder(
            bubble,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.08f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.08f),
          ).apply {
            duration = 450L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
          }
      } else {
        bubble.scaleX = 1f
        bubble.scaleY = 1f
        bubbleBorderDrawable?.setStroke(dp(2), Color.parseColor("#F5B82E"))
      }
    }

    for (holder in cardHolders) {
      val active = holder.soundId == playingId
      if (active) {
        holder.cardDrawable.setStroke(dp(2), Color.parseColor("#FFD54F"))
        holder.activeTintOverlay.visibility = View.VISIBLE
        holder.liveBadge.visibility = View.VISIBLE
        holder.equalizerRow.visibility = View.VISIBLE
        holder.playGlyph.text = "■ "
        holder.playGlyph.setTextColor(Color.parseColor("#FFD54F"))
        holder.titleTxt.setTextColor(Color.parseColor("#FFD54F"))
        holder.bottomBar.setBackgroundColor(Color.parseColor("#E60A0B10"))

        val cardPulse =
          ObjectAnimator.ofPropertyValuesHolder(
            holder.rootCard,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.035f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.035f),
          ).apply {
            duration = 480L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
          }
        activeAnimators.add(cardPulse)

        val durations = listOf(220L, 310L, 260L, 340L, 240L, 290L)
        holder.eqBars.forEachIndexed { idx, bar ->
          val anim =
            ObjectAnimator.ofFloat(bar, View.SCALE_Y, 0.25f, 1.15f).apply {
              duration = durations[idx % durations.size]
              startDelay = (idx * 45L)
              repeatCount = ValueAnimator.INFINITE
              repeatMode = ValueAnimator.REVERSE
              start()
            }
          activeAnimators.add(anim)
        }
      } else {
        holder.cardDrawable.setStroke(dp(1), Color.parseColor("#B8860B"))
        holder.rootCard.scaleX = 1f
        holder.rootCard.scaleY = 1f
        holder.activeTintOverlay.visibility = View.GONE
        holder.liveBadge.visibility = View.GONE
        holder.equalizerRow.visibility = View.GONE
        holder.playGlyph.text = "▶ "
        holder.playGlyph.setTextColor(Color.parseColor("#F5B82E"))
        holder.titleTxt.setTextColor(Color.WHITE)
        holder.bottomBar.setBackgroundColor(Color.parseColor("#CC08090C"))
      }
    }
  }

  private fun cancelCardAnimators() {
    activeAnimators.forEach { it.cancel() }
    activeAnimators.clear()
  }

  private fun createCircleIconBtn(label: String, onClick: () -> Unit): TextView {
    return TextView(this).apply {
      text = label
      setTextColor(Color.parseColor("#F5B82E"))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
      typeface = Typeface.DEFAULT_BOLD
      gravity = Gravity.CENTER
      background =
        GradientDrawable().apply {
          shape = GradientDrawable.OVAL
          setColor(Color.parseColor("#181A22"))
          setStroke(dp(1), Color.parseColor("#66F5B82E"))
        }
      layoutParams =
        LinearLayout.LayoutParams(dp(22), dp(22)).apply { marginStart = dp(4) }
      setOnClickListener { onClick() }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    particlesView?.stopAnim()
    cancelCardAnimators()
    bubblePulseAnimator?.cancel()
    FloatingOverlayBus.setRunning(false)
    serviceScope.cancel()
    rootContainer?.let { runCatching { windowManager?.removeView(it) } }
    rootContainer = null
  }

  private fun dp(value: Int): Int =
    TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_DIP,
      value.toFloat(),
      resources.displayMetrics,
    ).toInt()
}
