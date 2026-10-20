package com.moto.service;

import com.moto.model.Buyer;
import com.moto.model.FinancingPlan;
import com.moto.model.Motorcycle;
import com.moto.model.Payment;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class PdfReceiptServiceTest {

    @Test
    public void testDateFormat() {
        Payment payment = new Payment();
        payment.setFechaPago(LocalDateTime.of(2026, 10, 19, 16, 43, 19));
        assertEquals("19/10/2026 04:43:19 PM", payment.getFechaPagoFormatted());

        Payment morningPayment = new Payment();
        morningPayment.setFechaPago(LocalDateTime.of(2026, 10, 19, 9, 5, 2));
        assertEquals("19/10/2026 09:05:02 AM", morningPayment.getFechaPagoFormatted());
    }

    @Test
    public void testGenerateReceiptPdfWithOverdueBalance() throws Exception {
        PdfReceiptService service = new PdfReceiptService();

        Buyer buyer = new Buyer("Carlos Perez", "10203040", "3001234567", "Calle 10 # 20-30", null, LocalDate.now(), null);
        FinancingPlan plan = new FinancingPlan("moto123", buyer, 10000000.0, 2000000.0, 10, 800000.0, "MENSUAL", LocalDate.now().minusMonths(3));
        plan.setId("plan12345678");
        plan.setCuotasPagadas(2);
        plan.setCuotasAtrasadas(1);
        plan.setValorTotalAdeudado(800000.0);

        Motorcycle moto = new Motorcycle();
        moto.setId("moto123");
        moto.setMarca("AKT");
        moto.setModelo("NKD 125");
        moto.setPlaca("IGJ18I");

        Payment payment = new Payment();
        payment.setId("CEE45CB5");
        payment.setFinancingPlanId(plan.getId());
        payment.setFechaPago(LocalDateTime.now());
        payment.setValorPagado(35000.0);
        payment.setNumeroCuota(82);
        payment.setMetodoPago("NEQUI");
        payment.setObservaciones("Pago parcial");

        byte[] pdfBytes = service.generateReceiptPdf(payment, plan, moto);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        // Valid PDF starts with %PDF-
        String header = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", header);
    }

    @Test
    public void testGenerateReceiptPdfWithoutOverdueBalance() throws Exception {
        PdfReceiptService service = new PdfReceiptService();

        Buyer buyer = new Buyer("Carlos Perez", "10203040", "3001234567", "Calle 10 # 20-30", null, LocalDate.now(), null);
        FinancingPlan plan = new FinancingPlan("moto123", buyer, 10000000.0, 2000000.0, 10, 800000.0, "MENSUAL", LocalDate.now());
        plan.setId("plan12345678");
        plan.setCuotasPagadas(1);
        plan.setCuotasAtrasadas(0);
        plan.setValorTotalAdeudado(0.0);

        Motorcycle moto = new Motorcycle();
        moto.setId("moto123");
        moto.setMarca("Yamaha");
        moto.setModelo("FZ 25");
        moto.setPlaca("ABC12D");

        Payment payment = new Payment();
        payment.setId("REC123456");
        payment.setFinancingPlanId(plan.getId());
        payment.setFechaPago(LocalDateTime.now());
        payment.setValorPagado(800000.0);
        payment.setNumeroCuota(1);
        payment.setMetodoPago("EFECTIVO");

        byte[] pdfBytes = service.generateReceiptPdf(payment, plan, moto);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        String header = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", header);
    }
}
