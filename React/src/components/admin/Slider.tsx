const Slider = ({
                    value,
                    min = 5,
                    max = 100,
                    step = 5,
                    onChange,
                }: {
    value: number;
    min?: number;
    max?: number;
    step?: number;
    onChange: (value: number) => void;
}) => (
    <div className="flex items-center gap-4">
        <input
            type="range"
            min={min}
            max={max}
            step={step}
            value={value}
            onChange={(e) => onChange(Number(e.target.value))}
            className="w-full accent-server-color"
        />
        <span className="w-12 text-right font-semibold">{value}</span>
    </div>
);

export default Slider;