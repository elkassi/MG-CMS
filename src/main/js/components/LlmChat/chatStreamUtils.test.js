import { extractSseData, splitSseEvents } from './chatStreamUtils';

describe('splitSseEvents', () => {
    it('keeps incomplete SSE events buffered until the separator arrives', () => {
        const firstChunk = splitSseEvents('event: token\ndata: how');

        expect(firstChunk.events).toEqual([]);
        expect(firstChunk.remainingBuffer).toBe('event: token\ndata: how');

        const secondChunk = splitSseEvents(
            `${firstChunk.remainingBuffer} may I assist you?\n\nevent: done\ndata: [DONE]\n\n`
        );

        expect(secondChunk.events).toEqual([
            'event: token\ndata: how may I assist you?',
            'event: done\ndata: [DONE]'
        ]);
        expect(secondChunk.remainingBuffer).toBe('');
    });
});

describe('extractSseData', () => {
    it('preserves actual leading spaces in token payloads', () => {
        expect(extractSseData('event: token\ndata:  hello')).toBe(' hello');
    });

    it('joins multiline data fields with newlines', () => {
        expect(extractSseData('data: line 1\ndata: line 2')).toBe('line 1\nline 2');
    });
});