const Toggle = ({
                    checked,
                    onChange,
                    accent,
                }: {
    checked: boolean;
    onChange: (value: boolean) => void;
    accent: string;
}) => (
    <label className="inline-flex items-center cursor-pointer select-none">
        <input type="checkbox" className="sr-only" checked={checked} onChange={(e) => onChange(e.target.checked)} />
        <div className="w-11 h-6 bg-gray-300 rounded-full transition relative" style={{ backgroundColor: checked ? accent : undefined }}>
            <div className="absolute left-1 top-1 w-4 h-4 bg-white rounded-full transition" style={{ transform: checked ? 'translateX(20px)' : 'translateX(0)' }} />
        </div>
    </label>
);

export default Toggle;