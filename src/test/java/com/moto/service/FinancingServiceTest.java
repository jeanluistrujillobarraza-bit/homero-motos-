package com.moto.service;

import com.moto.model.Buyer;
import com.moto.model.FinancingPlan;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

public class FinancingServiceTest {

    @Test
    public void testCalculateEstimatedEndDate() {
        LocalDate start = LocalDate.of(2026, 6, 1);
        
        // Monthly: 12 months
        LocalDate endMonthly = FinancingPlan.calculateEstimatedEndDate(start, 12, "MENSUAL");
        assertEquals(LocalDate.of(2027, 6, 1), endMonthly);

        // Weekly: 4 weeks
        LocalDate endWeekly = FinancingPlan.calculateEstimatedEndDate(start, 4, "SEMANAL");
        assertEquals(LocalDate.of(2026, 6, 29), endWeekly);

        // Daily: 10 days
        LocalDate endDaily = FinancingPlan.calculateEstimatedEndDate(start, 10, "DIARIA");
        assertEquals(LocalDate.of(2026, 6, 11), endDaily);
    }

    @Test
    public void testRecalculate() {
        Buyer buyer = new Buyer("Test Buyer", "12345", "3000", "Calle 1", null, LocalDate.now(), null);
        
        // Total: $10,000,000
        // Down payment (cuota inicial): $2,000,000
        // Balance (saldo financiado): $8,000,000
        // 8 installments of $1,000,000
        FinancingPlan plan = new FinancingPlan("moto1", buyer, 10000000.0, 2000000.0, 8, 1000000.0, "MENSUAL", LocalDate.now());
        
        assertEquals(2000000.0, plan.getTotalPagado());
        assertEquals(8000000.0, plan.getSaldoPendiente());
        assertEquals(0, plan.getCuotasPagadas());
        assertEquals(8, plan.getCuotasRestantes());
        assertEquals(20.0, plan.getPorcentajeCancelado());
        
        // Register payment of $1,500,000 (which covers 1 full installment, leaving $500,000)
        plan.recalculate(1500000.0);
        
        assertEquals(3500000.0, plan.getTotalPagado());
        assertEquals(6500000.0, plan.getSaldoPendiente());
        assertEquals(1, plan.getCuotasPagadas());
        assertEquals(7, plan.getCuotasRestantes());
        
        // Pay remaining balance of $6,500,000
        plan.recalculate(6500000.0);
        
        assertEquals(10000000.0, plan.getTotalPagado());
        assertEquals(0.0, plan.getSaldoPendiente());
        assertEquals(8, plan.getCuotasPagadas());
        assertEquals(0, plan.getCuotasRestantes());
        assertEquals(100.0, plan.getPorcentajeCancelado());
        assertEquals("PAGADO", plan.getEstadoCredito());
    }
}
