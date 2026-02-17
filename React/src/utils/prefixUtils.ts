type HexColor = string;

const LEGACY_TO_HEX: Record<string, HexColor> = {
    "0": "#000000", "1": "#0000AA", "2": "#00AA00", "3": "#00AAAA",
    "4": "#AA0000", "5": "#AA00AA", "6": "#FFAA00", "7": "#AAAAAA",
    "8": "#555555", "9": "#5555FF", "a": "#55FF55", "b": "#55FFFF",
    "c": "#FF5555", "d": "#FF55FF", "e": "#FFFF55", "f": "#FFFFFF"
};

function replaceMinecraftHex(input: string): string {
    return input.replace(/&x(&[0-9a-fA-F]){6}/g, (match) => {
        let hex = "#";
        for (let i = 2; i < match.length; i += 2) {
            hex += match[i + 1];
        }
        return hex;
    });
}

function replaceLegacy(input: string): string {
    return input.replace(/[&§]([0-9a-fA-F])/g, (_, code) => {
        return LEGACY_TO_HEX[code.toLowerCase()] ?? "#FFFFFF";
    });
}

function parseText(input: string): HexColor[] {
    const result: HexColor[] = [];
    let currentColor: HexColor = "#FFFFFF";

    const gradientRegex = /<gradient:(#[0-9a-fA-F]{6}):(#[0-9a-fA-F]{6})>(.*?)<\/gradient>/g;

    let lastIndex = 0;
    let match: RegExpExecArray | null;

    while ((match = gradientRegex.exec(input)) !== null) {
        result.push(...parsePlain(input.substring(lastIndex, match.index), currentColor));
        result.push(...computeGradient(match[1], match[2], match[3]));

        lastIndex = match.index + match[0].length;
    }

    result.push(...parsePlain(input.substring(lastIndex), currentColor));
    return result;
}

function parsePlain(text: string, currentColor: HexColor): HexColor[] {
    const colors: HexColor[] = [];
    let i = 0;

    while (i < text.length) {
        if (text[i] === "#") {
            const hex = text.substring(i, i + 7);
            if (/^#[0-9a-fA-F]{6}$/.test(hex)) {
                currentColor = hex;
                i += 7;
                continue;
            }
        }

        colors.push(currentColor);
        i++;
    }

    return colors;
}

function computeGradient(start: HexColor, end: HexColor, text: string): HexColor[] {
    const startRGB = hexToRgb(start);
    const endRGB = hexToRgb(end);
    const colors: HexColor[] = [];

    for (let i = 0; i < text.length; i++) {
        const t = text.length === 1 ? 0 : i / (text.length - 1);
        const r = Math.round(startRGB.r + (endRGB.r - startRGB.r) * t);
        const g = Math.round(startRGB.g + (endRGB.g - startRGB.g) * t);
        const b = Math.round(startRGB.b + (endRGB.b - startRGB.b) * t);
        colors.push(rgbToHex(r, g, b));
    }

    return colors;
}

function hexToRgb(hex: HexColor) {
    const value = parseInt(hex.slice(1), 16);
    return {
        r: (value >> 16) & 255,
        g: (value >> 8) & 255,
        b: value & 255,
    };
}

function rgbToHex(r: number, g: number, b: number): HexColor {
    return (
        "#" +
        [r, g, b]
            .map((x) => x.toString(16).padStart(2, "0"))
            .join("")
            .toLowerCase()
    );
}

export function parsePrefixLabel(input: string): string {
    let label = input.replace(
        /<gradient:(#[0-9a-fA-F]{6}):(#[0-9a-fA-F]{6})>(.*?)<\/gradient>/g,
        (_match, _startColor, _endColor, textContent) => textContent
    );

    label = label.replace(/<[^>]+>/g, '');
    label = label.replace(/&x(&[0-9a-fA-F]){6}/g, '');
    label = label.replace(/[&§][0-9a-fk-orA-FK-OR]/g, '');
    label = label.replace(/#[0-9a-fA-F]{6}/g, '');

    return label;
}

export function parsePrefixColors(prefix: string): HexColor[] {
    prefix = replaceMinecraftHex(prefix);
    prefix = replaceLegacy(prefix);

    return parseText(prefix);
}