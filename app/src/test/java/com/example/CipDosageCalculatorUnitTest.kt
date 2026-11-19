package com.example

import com.example.util.CategoryIconResolver
import com.example.util.CipCleaningMode
import com.example.util.CipDosageCalculator
import com.example.util.CipEvaluationOutcome
import com.example.util.CipHousingSize
import com.example.util.CipValidationError
import com.example.util.PerawatanIconType
import com.example.util.RoMembraneCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test murni untuk [CipDosageCalculator] dan integrasi ikon riwayat CIP.
 */
class CipDosageCalculatorUnitTest {

    @Test
    fun `mode A citric acid 2 percent on 18 liters yields 360 grams and 20 grams per liter`() {
        val outcome = CipDosageCalculator.evaluateInputs(
            mode = CipCleaningMode.CITRIC_ACID,
            housingSize = CipHousingSize.HOUSING_4040,
            tubeCount = 1,
            waterVolumeRaw = "18",
            concentrationRaw = "2,0"
        )
        assertTrue(outcome is CipEvaluationOutcome.Success)
        val result = (outcome as CipEvaluationOutcome.Success).result
        assertEquals(360.0, result.dosageAmount, 0.001)
        assertEquals(20.0, result.dosagePerLiter, 0.001)
        assertEquals("gram", result.unit)
        assertEquals("pH 2,5 – 3,5", result.targetPhRange)
        assertEquals(35, result.maxTemperatureCelsius)
        assertFalse(result.isCustomWaterVolume)
        assertFalse(result.isOutsideRecommendedRange)
    }

    @Test
    fun `mode B caustic soda NaOH 0_1 percent on 18 liters yields 18 grams and 1 gram per liter`() {
        val outcome = CipDosageCalculator.evaluateInputs(
            mode = CipCleaningMode.CAUSTIC_SODA_NAOH,
            housingSize = CipHousingSize.HOUSING_4040,
            tubeCount = 1,
            waterVolumeRaw = "18",
            concentrationRaw = "0,1"
        )
        assertTrue(outcome is CipEvaluationOutcome.Success)
        val result = (outcome as CipEvaluationOutcome.Success).result
        assertEquals(18.0, result.dosageAmount, 0.001)
        assertEquals(1.0, result.dosagePerLiter, 0.001)
        assertEquals("gram", result.unit)
        assertEquals("pH 11,0 – 11,5 (Maks. 12,0)", result.targetPhRange)
        assertFalse(result.isOutsideRecommendedRange)
    }

    @Test
    fun `mode B caustic soda NaOH rejects concentration above 0_2 percent hard safety limit`() {
        val validAtLimit = CipDosageCalculator.evaluateInputs(
            mode = CipCleaningMode.CAUSTIC_SODA_NAOH,
            housingSize = CipHousingSize.HOUSING_4040,
            tubeCount = 1,
            waterVolumeRaw = "18",
            concentrationRaw = "0,2"
        )
        assertTrue(validAtLimit is CipEvaluationOutcome.Success)
        val atLimitResult = (validAtLimit as CipEvaluationOutcome.Success).result
        assertEquals(36.0, atLimitResult.dosageAmount, 0.001)
        assertTrue(atLimitResult.isOutsideRecommendedRange)

        val exceededOutcome = CipDosageCalculator.evaluateInputs(
            mode = CipCleaningMode.CAUSTIC_SODA_NAOH,
            housingSize = CipHousingSize.HOUSING_4040,
            tubeCount = 1,
            waterVolumeRaw = "18",
            concentrationRaw = "0,21"
        )
        assertTrue(exceededOutcome is CipEvaluationOutcome.ValidationError)
        val error = exceededOutcome as CipEvaluationOutcome.ValidationError
        assertNull(error.volumeError)
        assertEquals(CipValidationError.NAOH_EXCEEDS_MAX_SAFE_LIMIT, error.concentrationError)
    }

    @Test
    fun `mode C specialty liquid 3 percent on 18 liters yields 540 ml and 30 ml per liter`() {
        val outcome = CipDosageCalculator.evaluateInputs(
            mode = CipCleaningMode.SPECIALTY_LIQUID,
            housingSize = CipHousingSize.HOUSING_4040,
            tubeCount = 1,
            waterVolumeRaw = "18",
            concentrationRaw = "3.0"
        )
        assertTrue(outcome is CipEvaluationOutcome.Success)
        val result = (outcome as CipEvaluationOutcome.Success).result
        assertEquals(540.0, result.dosageAmount, 0.001)
        assertEquals(30.0, result.dosagePerLiter, 0.001)
        assertEquals("ml", result.unit)
        assertFalse(result.isOutsideRecommendedRange)
    }

    @Test
    fun `housing size suggestions multiply per tube and clamp invalid tube count`() {
        assertEquals(
            18.0,
            CipDosageCalculator.suggestWaterVolumeLiters(CipHousingSize.HOUSING_4040, 1),
            0.001
        )
        assertEquals(
            36.0,
            CipDosageCalculator.suggestWaterVolumeLiters(CipHousingSize.HOUSING_4040, 2),
            0.001
        )
        assertEquals(
            12.0,
            CipDosageCalculator.suggestWaterVolumeLiters(CipHousingSize.HOUSING_4021, 1),
            0.001
        )
        assertEquals(
            24.0,
            CipDosageCalculator.suggestWaterVolumeLiters(CipHousingSize.HOUSING_4021, 2),
            0.001
        )
        assertEquals(
            45.0,
            CipDosageCalculator.suggestWaterVolumeLiters(CipHousingSize.HOUSING_8040, 1),
            0.001
        )
        assertEquals(
            90.0,
            CipDosageCalculator.suggestWaterVolumeLiters(CipHousingSize.HOUSING_8040, 2),
            0.001
        )
        assertEquals(
            5.0,
            CipDosageCalculator.suggestWaterVolumeLiters(CipHousingSize.HOUSING_HOUSEHOLD, 1),
            0.001
        )
        assertEquals(
            15.0,
            CipDosageCalculator.suggestWaterVolumeLiters(CipHousingSize.HOUSING_HOUSEHOLD, 3),
            0.001
        )
        assertEquals(
            18.0,
            CipDosageCalculator.suggestWaterVolumeLiters(CipHousingSize.HOUSING_4040, 0),
            0.001
        )
        assertEquals(
            18.0,
            CipDosageCalculator.suggestWaterVolumeLiters(CipHousingSize.HOUSING_4040, -3),
            0.001
        )
    }

    @Test
    fun `indonesian comma and dot decimal parsing works reliably`() {
        assertEquals(1.5, CipDosageCalculator.parseDecimalInput("1,5") ?: -1.0, 0.0001)
        assertEquals(1.5, CipDosageCalculator.parseDecimalInput("1.5") ?: -1.0, 0.0001)
        assertEquals(0.05, CipDosageCalculator.parseDecimalInput("0,05") ?: -1.0, 0.0001)
        assertEquals(0.05, CipDosageCalculator.parseDecimalInput(" 0.05 ") ?: -1.0, 0.0001)
        assertNull(CipDosageCalculator.parseDecimalInput(""))
        assertNull(CipDosageCalculator.parseDecimalInput("   "))
        assertNull(CipDosageCalculator.parseDecimalInput("abc"))
    }

    @Test
    fun `empty zero or negative inputs return validation error and never NaN`() {
        val emptyOutcome = CipDosageCalculator.evaluateInputs(
            mode = CipCleaningMode.CITRIC_ACID,
            housingSize = CipHousingSize.HOUSING_4040,
            tubeCount = 1,
            waterVolumeRaw = "",
            concentrationRaw = ""
        )
        assertTrue(emptyOutcome is CipEvaluationOutcome.ValidationError)
        val emptyErr = emptyOutcome as CipEvaluationOutcome.ValidationError
        assertEquals(CipValidationError.VOLUME_REQUIRED, emptyErr.volumeError)
        assertEquals(CipValidationError.CONCENTRATION_REQUIRED, emptyErr.concentrationError)

        val zeroOutcome = CipDosageCalculator.evaluateInputs(
            mode = CipCleaningMode.CITRIC_ACID,
            housingSize = CipHousingSize.HOUSING_4040,
            tubeCount = 1,
            waterVolumeRaw = "0",
            concentrationRaw = "0"
        )
        assertTrue(zeroOutcome is CipEvaluationOutcome.ValidationError)
        val zeroErr = zeroOutcome as CipEvaluationOutcome.ValidationError
        assertEquals(CipValidationError.VOLUME_MUST_BE_POSITIVE, zeroErr.volumeError)
        assertEquals(CipValidationError.CONCENTRATION_MUST_BE_POSITIVE, zeroErr.concentrationError)

        val negativeOutcome = CipDosageCalculator.evaluateInputs(
            mode = CipCleaningMode.CITRIC_ACID,
            housingSize = CipHousingSize.HOUSING_4040,
            tubeCount = 1,
            waterVolumeRaw = "-10",
            concentrationRaw = "-1,5"
        )
        assertTrue(negativeOutcome is CipEvaluationOutcome.ValidationError)
        val negErr = negativeOutcome as CipEvaluationOutcome.ValidationError
        assertEquals(CipValidationError.VOLUME_CANNOT_BE_NEGATIVE, negErr.volumeError)
        assertEquals(CipValidationError.CONCENTRATION_CANNOT_BE_NEGATIVE, negErr.concentrationError)

        assertEquals(0.0, CipDosageCalculator.calculateSolidWeightGrams(0.0, 2.0), 0.001)
        assertEquals(0.0, CipDosageCalculator.calculateSolidWeightGrams(-5.0, 2.0), 0.001)
        assertEquals(0.0, CipDosageCalculator.calculateLiquidVolumeMl(0.0, 3.0), 0.001)
        assertEquals(0.0, CipDosageCalculator.calculateLiquidVolumeMl(18.0, Double.NaN), 0.001)
    }

    @Test
    fun `housing size two way mapping maps 4040 4021 8040 and rumahan 1 to 1`() {
        assertEquals(
            CipHousingSize.HOUSING_4040,
            CipHousingSize.fromRoMembraneCategory(RoMembraneCategory.SIZE_4040)
        )
        assertEquals(
            CipHousingSize.HOUSING_4021,
            CipHousingSize.fromRoMembraneCategory(RoMembraneCategory.SIZE_4021)
        )
        assertEquals(
            CipHousingSize.HOUSING_8040,
            CipHousingSize.fromRoMembraneCategory(RoMembraneCategory.SIZE_8040)
        )
        assertEquals(
            CipHousingSize.HOUSING_HOUSEHOLD,
            CipHousingSize.fromRoMembraneCategory(RoMembraneCategory.RUMAHAN)
        )
        assertEquals(
            RoMembraneCategory.SIZE_4040,
            CipHousingSize.HOUSING_4040.toRoMembraneCategory()
        )
        assertEquals(
            RoMembraneCategory.SIZE_4021,
            CipHousingSize.HOUSING_4021.toRoMembraneCategory()
        )
        assertEquals(
            RoMembraneCategory.SIZE_8040,
            CipHousingSize.HOUSING_8040.toRoMembraneCategory()
        )
        assertEquals(
            RoMembraneCategory.RUMAHAN,
            CipHousingSize.HOUSING_HOUSEHOLD.toRoMembraneCategory()
        )
    }

    @Test
    fun `cuci membran ro cip history title resolves to membran ro icon`() {
        val iconType = CategoryIconResolver.resolvePerawatanIconType(
            syncId = "",
            nama = "Cuci Membran RO (CIP)"
        )
        assertEquals(PerawatanIconType.MEMBRAN_RO, iconType)

        val tankIconType = CategoryIconResolver.resolvePerawatanIconType(
            syncId = "",
            nama = "Cuci Tangki Air Baku"
        )
        assertEquals(PerawatanIconType.CUCI_TANGKI, tankIconType)
    }
}
