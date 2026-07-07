package com.lear.MGCMS.services.dispatcher;

import com.lear.MGCMS.services.dispatcher.SequenceDispatcherService.DispatchPreview;

/**
 * Combined response for the dispatcher preview endpoint that returns both
 * the zone breakdown ({@link DispatchPreview}) and the heatmap load matrix.
 */
public final class PreviewWithLoad {

    private final DispatchPreview preview;
    private final ZoneLoadDto load;

    public PreviewWithLoad(DispatchPreview preview, ZoneLoadDto load) {
        this.preview = preview;
        this.load = load;
    }

    public DispatchPreview getPreview() {
        return preview;
    }

    public ZoneLoadDto getLoad() {
        return load;
    }
}
