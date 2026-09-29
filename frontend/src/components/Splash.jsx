import StrokeText from './StrokeText.jsx'

// Full-screen intro shown once per page load (not on route changes).
// Plays the JAVA stroke-draw animation, then fades out via CSS.
export default function Splash() {
  return (
    <div className="splash" aria-hidden="true">
      <div className="splash-inner">
        <StrokeText
          text="JAVA"
          strokeColor="#38bdf8"
          fillColor="#f8fafc"
          strokeWidth={1.4}
          drawDuration={1.6}
          fillDelay={0.2}
          stagger={0.05}
          ease="power2.out"
          trigger="mount"
          fillMode="wipe"
          fontSize={128}
          fontWeight={800}
          letterSpacing={-4}
          reverse={false}
        />
        <p className="splash-sub">Campus Canteen Pre-Order</p>
      </div>
    </div>
  )
}
