package com.liquidlabs.log.search.handlers;

import com.liquidlabs.common.DateUtil;
import com.liquidlabs.common.MurmurHash3;
import com.liquidlabs.common.regex.MatchResult;
import com.liquidlabs.log.fields.FieldSet;
import com.liquidlabs.log.index.LogFile;
import com.liquidlabs.log.search.HistoEvent;
import com.liquidlabs.log.search.Query;
import com.liquidlabs.log.space.AggSpace;
import com.liquidlabs.log.space.LogRequest;

/**
 * Created with IntelliJ IDEA.
 * User: neil
 * Date: 01/07/2014
 * Time: 10:06
 * To change this template use File | Settings | File Templates.
 */
public class SummaryBucketHandler implements LogScanningHandler  {

    private final String requestHash;
    private LogScanningHandler next;
    private final LogRequest request;
    private SummaryBucket summaryBucket;
    private AggSpace aggSpace;
    private LogFile logFile;
    private long eventTime;
    private long lastEventTime;

    public SummaryBucketHandler(LogScanningHandler next, LogRequest request, AggSpace aggSpace, LogFile logFile) {
        this.next = next;
        this.request = request;
        this.requestHash = MurmurHash3.hashString(request.toString(), 10) + "";

        this.aggSpace = aggSpace;
        this.logFile = logFile;
    }
    @Override
    public void handle(FieldSet fieldSet, String[] fields, Query query, HistoEvent histo, long bucketTime, int lineNumber, long fileStartTime, long fileEndTime, long eventTime, String lineData, MatchResult matchResult, long requestStartTimeMs, long requestEndTimeMs, long bucketWidth) {

        this.eventTime = DateUtil.floorMin(eventTime);

        try {
            summaryBucket().increment(fieldSet, fields, "", "", eventTime, fileStartTime, fileEndTime, lineNumber, lineData, matchResult, true, request.getStartTimeMs(), request.getEndTimeMs());
        } finally {

            if (next != null) next.handle(fieldSet, fields, query, histo, bucketTime, lineNumber, fileStartTime, fileEndTime, eventTime, lineData, matchResult, requestStartTimeMs, requestEndTimeMs, bucketWidth);
        }
    }

    final private SummaryBucket summaryBucket() {
        if (summaryBucket == null) {
            summaryBucket = new SummaryBucket(this.request.subscriber(), this.aggSpace);
            summaryBucket.setMultiTypes(request.isMutliTypes());
        }
        return summaryBucket;
    }

    private void writeSummary(SummaryBucket summaryBucket, long eventTime) {
        if (summaryBucket != null) aggSpace.writeSummary(summaryBucket, true, requestHash, logFile.getId(), eventTime);
    }

    @Override
    public void flush() {
        writeSummary(summaryBucket, lastEventTime);
        if (next != null) next.flush();
    }

    @Override
    public LogScanningHandler copy() {
        LogScanningHandler nnn = next != null ? next.copy() : null;
        return new SummaryBucketHandler(nnn, request, aggSpace, logFile);
    }

    @Override
    public LogScanningHandler next() {
        return next;
    }

    public SummaryBucket getSummaryBucket() {
        return summaryBucket();
    }
}
