package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.domain.dispatcher.UnassignableSerie;
import com.lear.MGCMS.domain.dispatcher.UnassignableSerie.ReasonCode;
import com.lear.MGCMS.repositories.dispatcher.UnassignableSerieRepository;

/**
 * Partitions a batch of {@link SerieDispatchInfo} into "can be scheduled"
 * and "cannot be placed" for a target (date, shift), persisting the latter
 * as {@link UnassignableSerie} audit rows.
 *
 * <p>Delegates the per-serie resolution to {@link SerieZoneResolver}; this
 * class exists so the engine has a single entry point and a single place
 * where audit writes happen.</p>
 *
 * <p>Contract: the caller decides what to do with the rejected list
 * (typically: surface on the Process page, leave serie status unchanged).
 * This class NEVER mutates {@code CuttingRequest} rows — that belongs to
 * {@code SequenceDispatcherService} (Phase 4).</p>
 */
@Service
public class SchedulableSerieFilter {

    @Autowired
    private SerieZoneResolver serieZoneResolver;

    @Autowired
    private UnassignableSerieRepository unassignableRepository;

    /** Outcome of one call — schedulable series mapped to their resolved zone. */
    public static final class FilterResult {
        private final Map<SerieDispatchInfo, Zone> schedulable;
        private final List<Rejection> rejected;

        public FilterResult(Map<SerieDispatchInfo, Zone> schedulable, List<Rejection> rejected) {
            this.schedulable = schedulable;
            this.rejected = rejected;
        }

        public Map<SerieDispatchInfo, Zone> getSchedulable() { return schedulable; }
        public List<Rejection> getRejected() { return rejected; }
    }

    /** One rejected serie + why. */
    public static final class Rejection {
        private final SerieDispatchInfo serie;
        private final ReasonCode reasonCode;
        private final String reasonDetail;

        public Rejection(SerieDispatchInfo serie, ReasonCode reasonCode, String reasonDetail) {
            this.serie = serie;
            this.reasonCode = reasonCode;
            this.reasonDetail = reasonDetail;
        }

        public SerieDispatchInfo getSerie() { return serie; }
        public ReasonCode getReasonCode() { return reasonCode; }
        public String getReasonDetail() { return reasonDetail; }
    }

    /**
     * Resolves a zone for every serie in {@code series}, writing an audit
     * row for each one that can't be placed.
     *
     * @return schedulable series mapped to their target zone, plus a list
     *         of rejections in the same order the audit rows were written.
     */
    @Transactional
    public FilterResult filter(List<SerieDispatchInfo> series, LocalDate date, int shift) {
        Map<SerieDispatchInfo, Zone> ok = new LinkedHashMap<>();
        List<Rejection> ko = new ArrayList<>();
        if (series == null || series.isEmpty()) {
            return new FilterResult(ok, ko);
        }
        for (SerieDispatchInfo s : series) {
            if (s == null) continue;
            SerieZoneResolver.Resolution res = serieZoneResolver.resolve(s, date, shift);
            if (res.isAccepted()) {
                ok.put(s, res.getZone());
            } else {
                ReasonCode rc = map(res.getFailureReason());
                String detail = buildDetail(s, res.getFailureReason(), date, shift);
                unassignableRepository.save(new UnassignableSerie(s.getSerieId(), rc, detail));
                ko.add(new Rejection(s, rc, detail));
            }
        }
        return new FilterResult(ok, ko);
    }

    private static ReasonCode map(SerieZoneResolver.FailureReason fr) {
        if (fr == null) return ReasonCode.OTHER;
        switch (fr) {
            case NO_ZONE_ACCEPTING_TYPE:    return ReasonCode.NO_ZONE_ACCEPTING_TYPE;
            case ALL_ZONES_CLOSED_FOR_SHIFT: return ReasonCode.ALL_ZONES_CLOSED_FOR_SHIFT;
            case NO_ACTIVE_MACHINE_IN_ZONE:  return ReasonCode.NO_ACTIVE_MACHINE_IN_ZONE;
            default: return ReasonCode.OTHER;
        }
    }

    private static String buildDetail(SerieDispatchInfo s,
                                      SerieZoneResolver.FailureReason fr,
                                      LocalDate date,
                                      int shift) {
        String machine = s.getMachine() == null ? "<null>" : s.getMachine();
        return "serie=" + s.getSerieId()
             + " machineType=" + machine
             + " date=" + date
             + " shift=" + shift
             + " reason=" + fr;
    }
}
