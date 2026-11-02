interface RadialGaugeProps {
  value: number; // 0 to 100
  label: string;
  size?: number;
  suffix?: string;
  sublabel?: string;
}

export default function RadialGauge({
  value,
  label,
  size = 130,
  suffix = '%',
  sublabel,
}: RadialGaugeProps) {
  const clamped = Math.min(100, Math.max(0, isNaN(value) ? 0 : value));

  // Dynamic Neon Color (Grafana threshold logic)
  const color =
    clamped >= 95
      ? '#10B981' // Neon Green
      : clamped >= 80
      ? '#F59E0B' // Amber Warning
      : '#EF4444'; // Red Critical

  const strokeWidth = 9;
  const radius = (size - strokeWidth * 2) / 2;
  const circumference = Math.PI * radius; // Half-circle
  const strokeDashoffset = circumference - (clamped / 100) * circumference;

  return (
    <div className="flex flex-col items-center justify-center p-3 rounded-lg bg-[#111827]/80 border border-slate-800/80 shadow-inner">
      <div className="relative flex items-center justify-center" style={{ width: size, height: size / 1.7 }}>
        <svg
          width={size}
          height={size / 1.6}
          viewBox={`0 0 ${size} ${size / 1.6}`}
          className="overflow-visible"
        >
          {/* Background Track */}
          <path
            d={`M ${strokeWidth} ${size / 2} A ${radius} ${radius} 0 0 1 ${size - strokeWidth} ${size / 2}`}
            fill="none"
            stroke="#1E293B"
            strokeWidth={strokeWidth}
            strokeLinecap="round"
          />

          {/* Active Colored Arc with Neon Glow */}
          <path
            d={`M ${strokeWidth} ${size / 2} A ${radius} ${radius} 0 0 1 ${size - strokeWidth} ${size / 2}`}
            fill="none"
            stroke={color}
            strokeWidth={strokeWidth}
            strokeDasharray={circumference}
            strokeDashoffset={strokeDashoffset}
            strokeLinecap="round"
            style={{
              transition: 'stroke-dashoffset 0.8s ease-in-out, stroke 0.5s ease',
              filter: `drop-shadow(0 0 8px ${color}99)`,
            }}
          />
        </svg>

        {/* Big Number in Center */}
        <div className="absolute top-1/2 left-0 right-0 -translate-y-2 text-center">
          <span className="text-xl font-black tracking-tight text-white font-mono drop-shadow">
            {clamped.toFixed(1)}
            <span className="text-xs text-slate-400 font-sans ml-0.5">{suffix}</span>
          </span>
          {sublabel && (
            <span className="block text-[10px] text-slate-500 font-mono -mt-0.5">{sublabel}</span>
          )}
        </div>
      </div>

      {/* Label Underneath */}
      <span className="mt-2 text-[10px] font-bold uppercase tracking-wider text-slate-400">
        {label}
      </span>
    </div>
  );
}

