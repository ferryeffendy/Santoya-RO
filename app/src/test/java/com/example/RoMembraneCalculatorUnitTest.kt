package com.example

import com.example.util.RoDiagnosisCause
import com.example.util.RoMembraneCalculator
import com.example.util.RoMembraneCategory
import com.example.util.RoMembraneEvaluationOutcome
import com.example.util.RoMembraneStatus
import com.example.util.RoValidationFieldError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoMembraneCalculatorUnitTest {

    @Test
    fun `healthy membrane returns SEHAT and OPTIMAL when rejection above 96 and flow drop below 15`() {
        val result = RoMembraneCalculator.evaluate(
            tdsBakuPpm = 500.0,
            tdsJadiPpm = 10.0, // Rejeksi 98.0%
            gpdPerUnit = 2000.0, // 4040 XLP
            actualFlowLpm = 10.0,
            parallelCount = 2, // 4000 GPD -> 10.5150 LPM -> drop ~4.9%
            presetTypeName = "4040 XLP",
            presetMaxFeedTdsPpm = 1000
        )

        assertEquals(98.0, result.rejectionPercent, 0.001)
        assertEquals(4000.0, requireNotNull(result.totalGpd), 0.001)
        assertEquals(10.5150, requireNotNull(result.standardFlowLpm), 0.01)
        assertEquals(4.90, requireNotNull(result.flowDropPercent), 0.05)
        assertEquals(RoMembraneStatus.SEHAT, result.status)
        assertEquals(RoMembraneStatus.SEHAT, result.tdsStatus)
        assertEquals(RoMembraneStatus.SEHAT, result.flowStatus)
        assertEquals(RoDiagnosisCause.OPTIMAL, result.diagnosisCause)
        assertFalse(result.isFeedTdsAbovePresetLimit)
        assertFalse(result.isTdsJadiHigherThanBaku)
        assertFalse(result.isFlowAboveStandard)
    }

    @Test
    fun `tds only mode evaluates rejection and compensates pure permeate up to 10 ppm`() {
        // High rejection (97%) -> SEHAT
        val healthy = RoMembraneCalculator.evaluate(tdsBakuPpm = 400.0, tdsJadiPpm = 12.0)
        assertNull(healthy.flowDropPercent)
        assertNull(healthy.flowStatus)
        assertEquals(RoMembraneStatus.SEHAT, healthy.status)
        assertEquals(RoDiagnosisCause.OPTIMAL, healthy.diagnosisCause)

        // Low-mineral feed water (120 ppm -> 6 ppm = 95.0% rejection, permeate <= 10 ppm) -> SEHAT
        val lowFeedPurePermeate = RoMembraneCalculator.evaluate(tdsBakuPpm = 120.0, tdsJadiPpm = 6.0)
        assertEquals(95.0, lowFeedPurePermeate.rejectionPercent, 0.001)
        assertEquals(RoMembraneStatus.SEHAT, lowFeedPurePermeate.status)
        assertEquals(RoDiagnosisCause.OPTIMAL, lowFeedPurePermeate.diagnosisCause)

        // Rejection 94% with permeate 24 ppm (> 10 ppm) -> WASPADA (TDS_DEGRADED)
        val warning = RoMembraneCalculator.evaluate(tdsBakuPpm = 400.0, tdsJadiPpm = 24.0)
        assertEquals(94.0, warning.rejectionPercent, 0.001)
        assertEquals(RoMembraneStatus.WASPADA, warning.status)
        assertEquals(RoDiagnosisCause.TDS_DEGRADED, warning.diagnosisCause)

        // Rejection 89% (< 90%) -> KRITIS even if permeate is low
        val critical = RoMembraneCalculator.evaluate(tdsBakuPpm = 400.0, tdsJadiPpm = 44.0)
        assertEquals(89.0, critical.rejectionPercent, 0.001)
        assertEquals(RoMembraneStatus.KRITIS, critical.status)
        assertEquals(RoDiagnosisCause.TDS_DEGRADED, critical.diagnosisCause)
    }

    @Test
    fun `boundary thresholds classify rejection and calibrated 15 to 25 flow drop accurately`() {
        // Rejection exact 96.0% with TDS Jadi 12 ppm (> 10 ppm) -> WASPADA
        val exact96 = RoMembraneCalculator.evaluate(tdsBakuPpm = 300.0, tdsJadiPpm = 12.0)
        assertEquals(RoMembraneStatus.WASPADA, exact96.status)
        assertEquals(RoDiagnosisCause.TDS_DEGRADED, exact96.diagnosisCause)

        // Rejection 96.1% -> SEHAT
        val above96 = RoMembraneCalculator.evaluate(tdsBakuPpm = 1000.0, tdsJadiPpm = 39.0)
        assertEquals(RoMembraneStatus.SEHAT, above96.status)

        // Rejection exact 90.0% with TDS Jadi 20 ppm -> WASPADA
        val exact90 = RoMembraneCalculator.evaluate(tdsBakuPpm = 200.0, tdsJadiPpm = 20.0)
        assertEquals(RoMembraneStatus.WASPADA, exact90.status)

        // Rejection 89.9% -> KRITIS
        val below90 = RoMembraneCalculator.evaluate(tdsBakuPpm = 1000.0, tdsJadiPpm = 101.0)
        assertEquals(RoMembraneStatus.KRITIS, below90.status)

        // Flow drop 12.0% (< 15%) with healthy TDS -> SEHAT
        val std1Unit2000 = RoMembraneCalculator.convertGpdToLpm(2000.0)
        val drop12 = RoMembraneCalculator.evaluate(
            tdsBakuPpm = 500.0,
            tdsJadiPpm = 5.0,
            gpdPerUnit = 2000.0,
            actualFlowLpm = std1Unit2000 * 0.88
        )
        assertEquals(RoMembraneStatus.SEHAT, drop12.status)
        assertEquals(RoDiagnosisCause.OPTIMAL, drop12.diagnosisCause)

        // Flow drop exact 15.0% with healthy TDS -> WASPADA & FLOW_DEGRADED
        val drop15 = RoMembraneCalculator.evaluate(
            tdsBakuPpm = 500.0,
            tdsJadiPpm = 5.0,
            gpdPerUnit = 2000.0,
            actualFlowLpm = std1Unit2000 * 0.85
        )
        assertEquals(RoMembraneStatus.WASPADA, drop15.status)
        assertEquals(RoMembraneStatus.SEHAT, drop15.tdsStatus)
        assertEquals(RoMembraneStatus.WASPADA, drop15.flowStatus)
        assertEquals(RoDiagnosisCause.FLOW_DEGRADED, drop15.diagnosisCause)

        // Flow drop exact 25.0% with healthy TDS -> WASPADA & FLOW_DEGRADED
        val drop25 = RoMembraneCalculator.evaluate(
            tdsBakuPpm = 500.0,
            tdsJadiPpm = 5.0,
            gpdPerUnit = 2000.0,
            actualFlowLpm = std1Unit2000 * 0.75
        )
        assertEquals(RoMembraneStatus.WASPADA, drop25.status)
        assertEquals(RoDiagnosisCause.FLOW_DEGRADED, drop25.diagnosisCause)

        // Flow drop > 25% (26%) with healthy TDS -> KRITIS & FLOW_DEGRADED
        val drop26 = RoMembraneCalculator.evaluate(
            tdsBakuPpm = 500.0,
            tdsJadiPpm = 5.0,
            gpdPerUnit = 2000.0,
            actualFlowLpm = std1Unit2000 * 0.74
        )
        assertEquals(RoMembraneStatus.KRITIS, drop26.status)
        assertEquals(RoMembraneStatus.SEHAT, drop26.tdsStatus)
        assertEquals(RoMembraneStatus.KRITIS, drop26.flowStatus)
        assertEquals(RoDiagnosisCause.FLOW_DEGRADED, drop26.diagnosisCause)

        // Both degraded -> BOTH_DEGRADED
        val bothDegraded = RoMembraneCalculator.evaluate(
            tdsBakuPpm = 500.0,
            tdsJadiPpm = 35.0, // 93% WASPADA
            gpdPerUnit = 2000.0,
            actualFlowLpm = std1Unit2000 * 0.70 // 30% drop KRITIS
        )
        assertEquals(RoMembraneStatus.KRITIS, bothDegraded.status)
        assertEquals(RoDiagnosisCause.BOTH_DEGRADED, bothDegraded.diagnosisCause)
    }

    @Test
    fun `evaluateInputs blocks TDS Jadi above TDS Baku and allows TDS only when preset GPD is filled`() {
        val zeroBaku = RoMembraneCalculator.evaluateInputs(
            tdsBakuRaw = "0",
            tdsJadiRaw = "10",
            gpdPerUnitRaw = "",
            actualFlowLpmRaw = ""
        )
        assertTrue(zeroBaku is RoMembraneEvaluationOutcome.ValidationError)
        assertEquals(
            RoValidationFieldError.MUST_BE_GREATER_THAN_ZERO,
            (zeroBaku as RoMembraneEvaluationOutcome.ValidationError).tdsBakuError
        )

        // TDS Jadi > TDS Baku must be blocked at validation
        val invertedTds = RoMembraneCalculator.evaluateInputs(
            tdsBakuRaw = "150",
            tdsJadiRaw = "200",
            gpdPerUnitRaw = "2000",
            actualFlowLpmRaw = ""
        )
        assertTrue(invertedTds is RoMembraneEvaluationOutcome.ValidationError)
        assertEquals(
            RoValidationFieldError.TDS_JADI_EXCEEDS_BAKU,
            (invertedTds as RoMembraneEvaluationOutcome.ValidationError).tdsJadiError
        )

        // Saved preset GPD filled + Flow empty must succeed in TDS-only mode (no error block)
        val savedGpdTdsOnly = RoMembraneCalculator.evaluateInputs(
            tdsBakuRaw = "1400",
            tdsJadiRaw = "15",
            gpdPerUnitRaw = "2000",
            actualFlowLpmRaw = "",
            parallelCount = 2,
            presetTypeName = "4040 XLP",
            presetPsiHint = "70–100 PSI",
            presetMaxFeedTdsPpm = 1000
        )
        assertTrue(savedGpdTdsOnly is RoMembraneEvaluationOutcome.Success)
        val tdsOnlyRes = (savedGpdTdsOnly as RoMembraneEvaluationOutcome.Success).result
        assertNull(tdsOnlyRes.flowDropPercent)
        assertTrue(tdsOnlyRes.isFeedTdsAbovePresetLimit)
        assertEquals(1000, tdsOnlyRes.presetMaxFeedTdsPpm)

        // Flow filled + GPD empty must still require GPD
        val onlyFlow = RoMembraneCalculator.evaluateInputs(
            tdsBakuRaw = "350",
            tdsJadiRaw = "10",
            gpdPerUnitRaw = "",
            actualFlowLpmRaw = "9,5"
        )
        assertTrue(onlyFlow is RoMembraneEvaluationOutcome.ValidationError)
        assertEquals(
            RoValidationFieldError.PAIR_REQUIRED_WITH_FLOW,
            (onlyFlow as RoMembraneEvaluationOutcome.ValidationError).gpdError
        )
    }

    @Test
    fun `comma decimal 2x 2400 GPD at 11_5 LPM calculates flow drop under 15 percent as SEHAT`() {
        val outcome = RoMembraneCalculator.evaluateInputs(
            tdsBakuRaw = "400",
            tdsJadiRaw = "10,5",
            gpdPerUnitRaw = "2400",
            actualFlowLpmRaw = "11,5",
            parallelCount = 2,
            presetTypeName = "4040 ULP/LP",
            presetMaxFeedTdsPpm = 2000
        )
        val result = (outcome as RoMembraneEvaluationOutcome.Success).result
        assertEquals(RoMembraneStatus.SEHAT, result.status)
        assertTrue(requireNotNull(result.flowDropPercent) < 15.0)
        assertFalse(result.isFeedTdsAbovePresetLimit)
    }

    @Test
    fun `stopwatch helper converts 19L gallon in 120 seconds to 9_5 LPM`() {
        val lpm = RoMembraneCalculator.calculateFlowLpmFromStopwatch(volumeLiters = 19.0, elapsedSeconds = 120.0)
        assertNotNull(lpm)
        assertEquals(9.5, requireNotNull(lpm), 0.0001)

        assertNull(RoMembraneCalculator.calculateFlowLpmFromStopwatch(volumeLiters = 19.0, elapsedSeconds = 0.0))
        assertNull(RoMembraneCalculator.calculateFlowLpmFromStopwatch(volumeLiters = -5.0, elapsedSeconds = 30.0))
    }

    @Test
    fun `buildHistoryNote formats parallel membrane config, PSI hint, and feed TDS limit warning`() {
        val xlp = requireNotNull(RoMembraneCalculator.findPresetById("4040_xlp"))
        val result = RoMembraneCalculator.evaluate(
            tdsBakuPpm = 1350.0, // Exceeds 1000 ppm limit for XLP
            tdsJadiPpm = 15.0,
            gpdPerUnit = xlp.gpdPerUnit.toDouble(),
            actualFlowLpm = 10.0,
            parallelCount = 2,
            presetTypeName = xlp.typeName,
            presetPsiHint = xlp.psiHint,
            presetMaxFeedTdsPpm = xlp.maxFeedTdsPpm
        )
        assertTrue(result.isFeedTdsAbovePresetLimit)

        val note = RoMembraneCalculator.buildHistoryNote(
            result = result,
            statusLabel = "Membran Sehat",
            diagnosisText = "Kualitas Air & Performa Membran Sangat Baik"
        )
        assertTrue(note.contains("Membran Sehat"))
        assertTrue(note.contains("2x 4040 XLP"))
        assertTrue(note.contains("70–100 PSI"))
        assertTrue(note.contains("4.000 GPD"))
        assertTrue(note.contains("Peringatan Spek"))
        assertTrue(note.contains("1.000 ppm"))
    }

    @Test
    fun `catalog provides 21 unique Indonesian RO presets across 4 size categories with PSI and brand metadata`() {
        val all = RoMembraneCalculator.POPULAR_PRESETS
        assertEquals(21, all.size)
        assertEquals(21, all.map { it.id }.toSet().size)
        assertTrue(
            all.all {
                it.gpdPerUnit > 0 &&
                    it.seriesCode.isNotBlank() &&
                    it.psiHint.isNotBlank() &&
                    it.maxFeedTdsHint.isNotBlank() &&
                    it.maxFeedTdsPpm >= 500 &&
                    it.shortTypeLabel.isNotBlank()
            }
        )

        val size4040 = RoMembraneCalculator.presetsForCategory(RoMembraneCategory.SIZE_4040)
        assertEquals(5, size4040.size)
        assertTrue(size4040.any { it.shortTypeLabel == "ULP" && it.gpdPerUnit == 2400 })
        assertTrue(size4040.any { it.shortTypeLabel == "LP" && it.gpdPerUnit == 2800 })
        assertTrue(size4040.any { it.shortTypeLabel == "XLP" && it.gpdPerUnit == 2000 })
        assertTrue(size4040.any { it.shortTypeLabel == "XLE" && it.gpdPerUnit == 2600 })
        assertTrue(size4040.any { it.shortTypeLabel == "TapTec" && it.gpdPerUnit == 2800 })

        assertEquals(2, RoMembraneCalculator.presetsForCategory(RoMembraneCategory.SIZE_4021).size)
        assertEquals(4, RoMembraneCalculator.presetsForCategory(RoMembraneCategory.SIZE_8040).size)
        assertEquals(10, RoMembraneCalculator.presetsForCategory(RoMembraneCategory.RUMAHAN).size)

        // Legacy preset IDs must resolve correctly
        assertEquals("4040_ulp", RoMembraneCalculator.findPresetById("4040_ulp_lp")?.id)
        assertEquals("4040_lp", RoMembraneCalculator.findPresetById("4040_lp21")?.id)
        assertEquals("8040_lp", RoMembraneCalculator.findPresetById("8040_lp21")?.id)
        assertEquals("8040_xlp", RoMembraneCalculator.findPresetById("8040_xlp12")?.id)
    }

    @Test
    fun `industrial 8040 and depot 4021 presets evaluate parallel flow conversion accurately`() {
        val ulp22 = requireNotNull(RoMembraneCalculator.findPresetById("8040_ulp22")) // 10500 GPD
        val industrialResult = RoMembraneCalculator.evaluate(
            tdsBakuPpm = 900.0,
            tdsJadiPpm = 18.0, // Rejeksi 98.0%
            gpdPerUnit = ulp22.gpdPerUnit.toDouble(),
            actualFlowLpm = 54.0,
            parallelCount = 2, // 21000 GPD -> 55.2039 LPM -> drop ~2.18%
            presetTypeName = ulp22.typeName,
            presetPsiHint = ulp22.psiHint,
            presetMaxFeedTdsPpm = ulp22.maxFeedTdsPpm
        )
        assertEquals(21000.0, requireNotNull(industrialResult.totalGpd), 0.001)
        assertEquals(RoMembraneStatus.SEHAT, industrialResult.status)

        val ulp4021 = requireNotNull(RoMembraneCalculator.findPresetById("4021_ulp")) // 950 GPD
        val depot4021Result = RoMembraneCalculator.evaluate(
            tdsBakuPpm = 500.0,
            tdsJadiPpm = 10.0,
            gpdPerUnit = ulp4021.gpdPerUnit.toDouble(),
            actualFlowLpm = 1.75, // Std 2.497 LPM -> drop ~29.9% (> 25%) -> KRITIS (FLOW_DEGRADED)
            parallelCount = 1,
            presetTypeName = ulp4021.typeName,
            presetPsiHint = ulp4021.psiHint,
            presetMaxFeedTdsPpm = ulp4021.maxFeedTdsPpm
        )
        assertEquals(RoMembraneStatus.KRITIS, depot4021Result.status)
        assertEquals(RoDiagnosisCause.FLOW_DEGRADED, depot4021Result.diagnosisCause)
    }
}
