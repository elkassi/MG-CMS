export const splitSseEvents = (buffer) => {
    const segments = buffer.split(/\r?\n\r?\n/);

    return {
        events: segments.slice(0, -1).filter(Boolean),
        remainingBuffer: segments[segments.length - 1] || ''
    };
};

/**
 * Extract the data payload from an SSE event block.
 * Spring's SseEmitter does NOT add a space after "data:", so the raw value
 * starts at index 5. Only strip the OPTIONAL single space defined by the SSE
 * spec (RFC 8895 §9.2.4) — never more than one — by using slice, not replace.
 */
export const extractSseData = (eventBlock) => eventBlock
    .split(/\r?\n/)
    .filter(line => line.startsWith('data:'))
    .map(line => {
        const raw = line.slice(5);
        // SSE spec: if the first char after "data:" is a space, it is a separator (skip it).
        // But Spring SseEmitter doesn't always add it, so only strip if present AND
        // the remaining data is non-empty (avoid erasing a space-only token).
        if (raw.length > 1 && raw.charAt(0) === ' ') {
            return raw.slice(1);
        }
        return raw;
    })
    .join('\n');