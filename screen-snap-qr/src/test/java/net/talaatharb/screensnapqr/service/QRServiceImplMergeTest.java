package net.talaatharb.screensnapqr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;

class QRServiceImplMergeTest {

    @Test
    void mergeUniqueResultsRemovesDuplicateAcrossPhases() {
        final Result sharedInPrimary = new Result("same-text", new byte[] { 1, 2, 3 },
                new ResultPoint[] { new ResultPoint(10.0F, 20.0F) }, BarcodeFormat.QR_CODE);
        final Result uniquePrimary = new Result("primary-only", null, null, BarcodeFormat.CODE_128);
        final Result sharedInSecondary = new Result("same-text", new byte[] { 1, 2, 3 },
                new ResultPoint[] { new ResultPoint(10.0F, 20.0F) }, BarcodeFormat.QR_CODE);
        final Result uniqueSecondary = new Result("data-matrix", null, null, BarcodeFormat.DATA_MATRIX);

        final Result[] merged = QRServiceImpl.mergeUniqueResults(new Result[] { sharedInPrimary, uniquePrimary },
                new Result[] { sharedInSecondary, uniqueSecondary });

        assertEquals(3, merged.length);
    }

    @Test
    void mergeUniqueResultsCollapsesSameCodeDetectedAtSlightlyDifferentLocations() {
        // Simulates the same physical DataMatrix code detected by different
        // preprocessing variants (grayscale/otsu/upscaled/tiled), each yielding
        // slightly different result-point coordinates due to rounding/rescaling.
        final Result exactPass = new Result("same-payload", new byte[] { 1, 2, 3 },
                new ResultPoint[] { new ResultPoint(100.0F, 100.0F), new ResultPoint(150.0F, 150.0F) },
                BarcodeFormat.DATA_MATRIX);
        final Result slightlyShiftedPass = new Result("same-payload", new byte[] { 1, 2, 3 },
                new ResultPoint[] { new ResultPoint(103.0F, 98.0F), new ResultPoint(148.0F, 153.0F) },
                BarcodeFormat.DATA_MATRIX);

        final Result[] merged = QRServiceImpl.mergeUniqueResults(new Result[] { exactPass },
                new Result[] { slightlyShiftedPass });

        assertEquals(1, merged.length);
    }

    @Test
    void mergeUniqueResultsKeepsDistinctResultsAtClearlyDifferentLocations() {
        // Two on-screen occurrences of an identical payload, far apart, must
        // remain distinct results rather than being collapsed by dedup.
        final Result firstOccurrence = new Result("same-payload", new byte[] { 1, 2, 3 },
                new ResultPoint[] { new ResultPoint(10.0F, 10.0F), new ResultPoint(30.0F, 30.0F) },
                BarcodeFormat.DATA_MATRIX);
        final Result secondOccurrence = new Result("same-payload", new byte[] { 1, 2, 3 },
                new ResultPoint[] { new ResultPoint(800.0F, 600.0F), new ResultPoint(820.0F, 620.0F) },
                BarcodeFormat.DATA_MATRIX);

        final Result[] merged = QRServiceImpl.mergeUniqueResults(new Result[] { firstOccurrence },
                new Result[] { secondOccurrence });

        assertEquals(2, merged.length);
    }
}
