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
}
