/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.monitoring.web;

import static fish.payara.monitoring.web.ApiRequests.DataType.ALERTS;
import static fish.payara.monitoring.web.ApiRequests.DataType.POINTS;
import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import fish.payara.monitoring.alert.Watch;
import fish.payara.monitoring.adapt.GroupData;
import fish.payara.monitoring.alert.Alert;
import fish.payara.monitoring.alert.Alert.Level;
import fish.payara.monitoring.alert.AlertService.AlertStatistics;
import fish.payara.monitoring.alert.Circumstance;
import fish.payara.monitoring.alert.Condition;
import fish.payara.monitoring.model.AggregateDataset;
import fish.payara.monitoring.model.HoursDataset;
import fish.payara.monitoring.model.MinutesDataset;
import fish.payara.monitoring.model.SeriesAnnotation;
import fish.payara.monitoring.model.SeriesDataset;
import fish.payara.monitoring.web.ApiRequests.SeriesQuery;

/**
 * Types used in the web API to model mapped responses.
 *
 * The purpose of these classes is to decouple the API from internal server classes so that renaming or restructuring of
 * internal classes does not break the API.
 *
 * @see ApiRequests
 *
 * @author Jan Bernitt
 * @since 5.201
 */
@SuppressWarnings("squid:S1104")
public final class ApiResponses {

    /**
     * A {@link SeriesResponse} is the answer to a {@link ApiRequests.SeriesRequest}.
     *
     * It consists of an {@link Alerts} statistic and a {@link SeriesMatch} for each {@link SeriesQuery}.
     */
    public static final class SeriesResponse {

        public final Alerts alerts;
        public final List<SeriesMatch> matches;

        public SeriesResponse(List<SeriesMatch> matches, AlertStatistics alertStatistics) {
            this.alerts = new Alerts(alertStatistics);
            this.matches = matches;
        }
    }

    public static final class Alerts {

        public final int changeCount;
        public final int unacknowledgedRedAlerts;
        public final int acknowledgedRedAlerts;
        public final int unacknowledgedAmberAlerts;
        public final int acknowledgedAmberAlerts;
        public final int watchCount;
        public final int[] ongoingRedAlerts;
        public final int[] ongoingAmberAlerts;

        public Alerts(AlertStatistics stats) {
            this.watchCount = stats.watches;
            this.changeCount = stats.changeCount;
            this.unacknowledgedRedAlerts = stats.unacknowledgedRedAlerts;
            this.acknowledgedRedAlerts = stats.acknowledgedRedAlerts;
            this.unacknowledgedAmberAlerts = stats.unacknowledgedAmberAlerts;
            this.acknowledgedAmberAlerts = stats.acknowledgedAmberAlerts;
            this.ongoingRedAlerts = stats.ongoingRedAlerts;
            this.ongoingAmberAlerts = stats.ongoingAmberAlerts;
        }
    }

    /**
     * Corresponding answer to a {@link SeriesQuery}.
     */
    public static final class SeriesMatch {

        public final String widgetId;
        public final String series;
        public final List<SeriesData> data;
        public final List<AnnotationData> annotations;
        public final List<WatchData> watches;
        public final List<AlertData> alerts;

        public SeriesMatch(SeriesQuery query, String series, List<SeriesDataset> data, List<SeriesAnnotation> annotations, //
                Collection<Watch> watches, Collection<Alert> alerts) {
            this.widgetId = query.widgetId;
            this.series = series;
            this.alerts = alerts.stream().map(alert -> new AlertData(alert, query.truncates(ALERTS))).collect(toList());
            this.watches = watches.stream().map(WatchData::new).collect(toList());
            this.data = data.stream().map(set -> new SeriesData(set, query.truncates(POINTS), query.history)).collect(toList());
            this.annotations = annotations.stream().map(AnnotationData::new).collect(toList());
        }

        public SeriesMatch(String series, List<SeriesData> data, List<AnnotationData> annotations,
                List<WatchData> watches, List<AlertData> alerts) {
            this.widgetId = "grouped";
            this.series = series;
            this.data = data;
            this.annotations = annotations;
            this.watches = watches;
            this.alerts = alerts;
        }
    }

    public static final class AnnotationData {

        public final long time;
        public final String series;
        public final String instance;
        public final long value;
        public final Map<String, String> attrs;
        public final boolean permanent;

        AnnotationData(SeriesAnnotation annotation) {
            this.time = annotation.getTime();
            this.series = annotation.getSeries().toString();
            this.instance = annotation.getInstance();
            this.value = annotation.getValue();
            this.permanent = annotation.isPermanent();
            this.attrs = new LinkedHashMap<>(); // keep order
            for (Entry<String, String> attr : annotation) {
                attrs.put(attr.getKey(), attr.getValue());
            }
        }
    }

    public static final class WatchData {

        public String name;
        public String series;
        public String unit;
        public boolean stopped;
        public boolean disabled;
        public boolean programmatic;
        public CircumstanceData red;
        public CircumstanceData amber;
        public CircumstanceData green;
        /**
         * by series and instance
         */
        public final Map<String, Map<String, WatchState>> states = new HashMap<>();

        public WatchData() {
            // from JSON
        }

        public WatchData(Watch watch) {
            this.name = watch.name;
            this.series = watch.watched.series.toString();
            this.unit = watch.watched.unit.toString();
            this.stopped = watch.isStopped();
            this.disabled = watch.isDisabled();
            this.programmatic = watch.isProgrammatic();
            this.red = watch.red.isUnspecified() ? null : new CircumstanceData(watch.red);
            this.amber = watch.amber.isUnspecified() ? null : new CircumstanceData(watch.amber);
            this.green = watch.green.isUnspecified() ? null : new CircumstanceData(watch.green);
            for (Watch.State state : watch) {
                states.computeIfAbsent(state.getSeries().toString(), key -> new HashMap<>())
                    .put(state.getInstance(), new WatchState(state));
            }
        }
    }

    public static final class WatchState {

        public final String level;
        public final Long since;

        public WatchState(Watch.State state) {
            this.level = state.getLevel().name().toLowerCase();
            this.since = state.getSince();
        }
    }

    public static final class CircumstanceData {

        public String level;
        public ConditionData start;
        public ConditionData stop;
        public ConditionData suppress;
        public String surpressingSeries;
        public String surpressingUnit;

        public CircumstanceData() {
            // from JSON
        }

        public CircumstanceData(Circumstance circumstance) {
            this.level = circumstance.level.name().toLowerCase();
            this.start = circumstance.start.isNone() ? null : new ConditionData(circumstance.start);
            this.stop = circumstance.stop.isNone() ? null : new ConditionData(circumstance.stop);
            this.suppress = circumstance.suppress.isNone() ? null : new ConditionData(circumstance.suppress);
            this.surpressingSeries = circumstance.suppressing == null ? null : circumstance.suppressing.series.toString();
            this.surpressingUnit = circumstance.suppressing == null ? null : circumstance.suppressing.unit.toString();
        }
    }

    public static final class ConditionData {

        public String operator;
        public long threshold;
        public Integer forTimes;
        public Long forMillis;
        public boolean onAverage;

        public ConditionData() {
            // from JSON
        }

        public ConditionData(Condition condition) {
            this.operator = condition.comparison.toString();
            this.threshold = condition.threshold;
            this.forTimes = condition.isForLastTimes() ? condition.forLast.intValue() : null;
            this.forMillis = condition.isForLastMillis() ? condition.forLast.longValue() : null;
            this.onAverage = condition.onAverage;
        }
    }

    public static final class SeriesData {

        public final String series;
        public final String instance;
        public final long[] points;
        public final long observedMax;
        public final long observedMin;
        public final BigInteger observedSum;
        public final int observedValues;
        public final int observedValueChanges;
        public final long observedSince;
        public final int stableCount;
        public final long stableSince;
        public final AggregatedSeriesData minutes;
        public final AggregatedSeriesData hours;
        public final AggregatedSeriesData days;

        public SeriesData(SeriesDataset set) {
            this(set, false, false);
        }

        public SeriesData(SeriesDataset set, boolean truncatePoints, boolean history) {
            this.instance = set.getInstance();
            this.series = set.getSeries().toString();
            this.points = truncatePoints ? new long[] {set.lastTime(), set.lastValue()} : set.points();
            this.observedMax = set.getObservedMax();
            this.observedMin = set.getObservedMin();
            this.observedSum = set.getObservedSum();
            this.observedValues = set.getObservedValues();
            this.observedValueChanges = set.getObservedValueChanges();
            this.observedSince = set.getObservedSince();
            this.stableCount = set.getStableCount();
            this.stableSince = set.getStableSince();
            if (!history || truncatePoints) {
                this.minutes = null;
                this.hours = null;
                this.days = null;
            } else {
                MinutesDataset minutes = set.getRecentMinutes();
                this.minutes = AggregatedSeriesData.of(minutes);
                HoursDataset hours = minutes.getRecentHours();
                this.hours = AggregatedSeriesData.of(hours);
                this.days = AggregatedSeriesData.of(hours.getRecentDays());
            }
        }
    }

    public static final class RequestTraceResponse {

        public final UUID id;
        public final long startTime;
        public final long endTime;
        public final long elapsedTime;
        public final List<RequestTraceSpan> spans = new ArrayList<>();

        public RequestTraceResponse(GroupData data) {
            this.id = data.getField("id", UUID.class);
            this.startTime = data.getField("startTime", Long.class);
            this.endTime = data.getField("endTime", Long.class);
            this.elapsedTime = data.getField("elapsedTime", Long.class);
            for (GroupData span : data.getChildren().values()) {
                this.spans.add(new RequestTraceSpan(span));
            }
        }
    }

    public static final class RequestTraceSpan {

        public final UUID id;
        public final String operation;
        public final long startTime;
        public final long endTime;
        public final long duration;
        public final Map<String, String> tags;

        public RequestTraceSpan(GroupData span) {
            this.id = span.getField("id", UUID.class);
            this.operation = span.getField("operation", String.class);
            this.startTime = span.getField("startTime", Long.class);
            this.endTime = span.getField("endTime", Long.class);
            this.duration = span.getField("duration", Long.class);
            this.tags = new LinkedHashMap<>();
            for (Entry<String, Serializable> tag : span.getChildren().get("tags").getFields().entrySet()) {
                tags.put(tag.getKey(), tag.getValue().toString());
            }
        }
    }

    public static final class AlertsResponse {

        public final List<AlertData> alerts;

        public AlertsResponse(Collection<Alert> alerts) {
            this.alerts = alerts.stream().map(AlertData::new).collect(toList());
        }

    }

    public static final class AlertData {

        public final int serial;
        public final String level;
        public final String series;
        public final String instance;
        public final WatchData initiator;
        public final boolean acknowledged;
        public final boolean stopped;
        public final long since;
        public final Long until;
        public final List<AlertFrame> frames;

        public AlertData(Alert alert) {
            this(alert, false);
        }

        public AlertData(Alert alert, boolean truncateAlerts) {
            this.serial = alert.serial;
            this.level = alert.getLevel().name().toLowerCase();
            this.series = alert.getSeries().toString();
            this.instance = alert.getInstance();
            this.initiator = new WatchData(alert.initiator);
            this.acknowledged = alert.isAcknowledged();
            this.stopped = alert.isStopped();
            this.since = alert.getStartTime();
            this.until = alert.isStopped() ? alert.getEndTime() : null;
            this.frames = new ArrayList<>();
            if (truncateAlerts) {
                this.frames.add(new AlertFrame(alert.getEndFrame()));
            } else {
                for (Alert.Frame t : alert) {
                    this.frames.add(new AlertFrame(t));
                }
            }
        }
    }

    /**
     * Each time an {@link Alert} transitions between {@link Level#RED} and {@link Level#AMBER} a new frame is created
     * capturing the {@link AlertFrame#cause} of the transition as well as the {@link AlertFrame#captured} metrics.
     */
    public static final class AlertFrame {

        public final String level;
        public final SeriesData cause;
        public final List<SeriesData> captured;
        public final long start;
        public final Long end;

        public AlertFrame(Alert.Frame frame) {
            this.level = frame.level.name().toLowerCase();
            this.cause = new SeriesData(frame.cause, true, false); // for now the points data isn't used, change to false if needed
            this.start = frame.start;
            this.end = frame.getEnd() <= 0 ? null : frame.getEnd();
            this.captured = new ArrayList<>();
            for (SeriesDataset capture : frame) {
                this.captured.add(new SeriesData(capture, true, false)); // for now the points data isn't used, change to false if needed
            }
        }
    }

    public static final class WatchesResponse {
        public final List<WatchData> watches;

        public WatchesResponse(Collection<Watch> watches) {
            this.watches = watches.stream().map(WatchData::new).collect(toList());
        }
    }

    public static final class AggregatedSeriesData {

        static AggregatedSeriesData of(AggregateDataset<?> data) {
            return data.isEmpty() ? null : new AggregatedSeriesData(data);
        }

        public final long start;
        public final long interval;
        public final long[] mins;
        public final long[] maxs;
        public final double[] avgs;
        public final int[] points;

        public AggregatedSeriesData(AggregateDataset<?> data) {
            this.start = data.firstTime();
            this.interval = data.getIntervalLength();
            this.mins = data.mins();
            this.maxs = data.maxs();
            this.avgs = data.avgs();
            this.points = data.numberOfPoints();
        }
    }
}
