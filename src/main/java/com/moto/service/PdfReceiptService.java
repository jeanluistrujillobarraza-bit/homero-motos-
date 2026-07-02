package com.moto.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.moto.model.FinancingPlan;
import com.moto.model.Motorcycle;
import com.moto.model.Payment;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class PdfReceiptService {

    public byte[] generateReceiptPdf(Payment payment, FinancingPlan plan, Motorcycle moto) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        // Custom Page Size for 80mm roll printer paper (approx 226 points width)
        // Set a reasonable height (e.g. 580 points) to accommodate all details in a single page
        Rectangle ticketPageSize = new Rectangle(226f, 580f);
        
        // Very tight margins for standard ticket rolls
        Document document = new Document(ticketPageSize, 10, 10, 10, 10);
        PdfWriter.getInstance(document, out);

        document.open();

        // -------------------------------------------------------------
        // FONTS (Monochrome thermal printer style)
        // -------------------------------------------------------------
        Font brandFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLACK);
        Font titleFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.BLACK);
        Font boldFont = new Font(Font.HELVETICA, 7.5f, Font.BOLD, Color.BLACK);
        Font regularFont = new Font(Font.HELVETICA, 7.5f, Font.NORMAL, Color.BLACK);
        Font miniFont = new Font(Font.HELVETICA, 6.5f, Font.NORMAL, Color.BLACK);

        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
        nf.setMaximumFractionDigits(0);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        DateTimeFormatter dateOnlyFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // -------------------------------------------------------------
        // BRANDING & HEADER (Centered)
        // -------------------------------------------------------------
        Paragraph brandPara = new Paragraph();
        brandPara.setAlignment(Element.ALIGN_CENTER);
        brandPara.setLeading(10f);
        brandPara.add(new Chunk("*** HOMERO MOTOS ***\n", brandFont));
        brandPara.add(new Chunk("Tel: 3217765698\n", regularFont));
        brandPara.add(new Chunk("================================\n", regularFont));
        document.add(brandPara);

        // -------------------------------------------------------------
        // TRANSACTION HEADER
        // -------------------------------------------------------------
        Paragraph transPara = new Paragraph();
        transPara.setLeading(10f);
        
        String recId = payment.getId();
        String displayId = (recId != null && recId.length() >= 8) 
                ? recId.substring(recId.length() - 8).toUpperCase() 
                : "PENDIENTE";
        
        transPara.add(new Chunk("COMPROBANTE DE RECAUDO\n", titleFont));
        transPara.add(new Chunk("RECIBO N°: ", boldFont));
        transPara.add(new Chunk(displayId + "\n", boldFont));
        transPara.add(new Chunk("FECHA: ", boldFont));
        transPara.add(new Chunk(payment.getFechaPago().format(dtf) + "\n", regularFont));
        transPara.add(new Chunk("--------------------------------\n", regularFont));
        document.add(transPara);

        // -------------------------------------------------------------
        // CLIENT DETAILS
        // -------------------------------------------------------------
        Paragraph clientPara = new Paragraph();
        clientPara.setLeading(10f);
        clientPara.add(new Chunk("TITULAR: ", boldFont));
        clientPara.add(new Chunk(plan.getBuyer().getNombreCompleto().toUpperCase() + "\n", regularFont));
        clientPara.add(new Chunk("IDENTIFICACIÓN: ", boldFont));
        clientPara.add(new Chunk(plan.getBuyer().getCedula() + "\n", regularFont));
        clientPara.add(new Chunk("TELÉFONO: ", boldFont));
        clientPara.add(new Chunk(plan.getBuyer().getTelefono() + "\n", regularFont));
        if (plan.getBuyer().getDireccion() != null && !plan.getBuyer().getDireccion().trim().isEmpty()) {
            clientPara.add(new Chunk("DIRECCIÓN: ", boldFont));
            clientPara.add(new Chunk(plan.getBuyer().getDireccion().toUpperCase() + "\n", regularFont));
        }
        clientPara.add(new Chunk("--------------------------------\n", regularFont));
        document.add(clientPara);

        // -------------------------------------------------------------
        // VEHICLE DETAILS
        // -------------------------------------------------------------
        Paragraph vehiclePara = new Paragraph();
        vehiclePara.setLeading(10f);
        
        String motoMarca = (moto != null) ? moto.getMarca() : "Eliminada";
        String motoModelo = (moto != null) ? moto.getModelo() : "-";
        String motoPlaca = (moto != null && moto.getPlaca() != null) ? moto.getPlaca().toUpperCase() : "S/P";

        vehiclePara.add(new Chunk("VEHÍCULO: ", boldFont));
        vehiclePara.add(new Chunk(motoMarca.toUpperCase() + " " + motoModelo.toUpperCase() + "\n", regularFont));
        vehiclePara.add(new Chunk("PLACA: ", boldFont));
        vehiclePara.add(new Chunk(motoPlaca + "\n", boldFont));
        
        String planId = plan.getId();
        String displayPlanId = (planId != null && planId.length() >= 8) 
                ? planId.substring(planId.length() - 8).toUpperCase() 
                : "PENDIENTE";
        vehiclePara.add(new Chunk("CONTRATO N°: ", boldFont));
        vehiclePara.add(new Chunk(displayPlanId + "\n", regularFont));
        vehiclePara.add(new Chunk("--------------------------------\n", regularFont));
        document.add(vehiclePara);

        // -------------------------------------------------------------
        // PAYMENT DETAILS
        // -------------------------------------------------------------
        Paragraph payPara = new Paragraph();
        payPara.setLeading(10f);
        
        payPara.add(new Chunk("CONCEPTO: ", boldFont));
        payPara.add(new Chunk("Abono Cuota N° " + (payment.getNumeroCuota() == 0 ? "Inicial" : payment.getNumeroCuota()) + "\n", regularFont));
        payPara.add(new Chunk("MÉTODO PAGO: ", boldFont));
        payPara.add(new Chunk(payment.getMetodoPago().toUpperCase() + "\n", regularFont));
        document.add(payPara);
        
        // VALOR HIGHLIGHT BOX (Standard thermal format outline)
        PdfPTable valTable = new PdfPTable(1);
        valTable.setWidthPercentage(100);
        valTable.setSpacingBefore(4f);
        valTable.setSpacingAfter(4f);
        
        Paragraph valPara = new Paragraph();
        valPara.setAlignment(Element.ALIGN_CENTER);
        valPara.setLeading(12f);
        valPara.add(new Chunk("VALOR PAGADO\n", boldFont));
        valPara.add(new Chunk(nf.format(payment.getValorPagado()), brandFont));
        
        PdfPCell valCell = new PdfPCell(valPara);
        valCell.setBorder(PdfPCell.BOX);
        valCell.setBorderColor(Color.BLACK);
        valCell.setBorderWidth(1.2f);
        valCell.setPadding(5f);
        valTable.addCell(valCell);
        document.add(valTable);

        // -------------------------------------------------------------
        // CREDIT SUMMARY STATUS
        // -------------------------------------------------------------
        Paragraph summaryPara = new Paragraph();
        summaryPara.setLeading(10f);
        summaryPara.add(new Chunk("================================\n", regularFont));
        summaryPara.add(new Chunk("ESTADO DE CRÉDITO\n", titleFont));
        
        int totalCuotas = (plan.getCuotasTotales() != null) ? plan.getCuotasTotales() : 0;
        int cuotasPagadas = (plan.getCuotasPagadas() != null) ? plan.getCuotasPagadas() : 0;
        int cuotasRestantes = totalCuotas - cuotasPagadas;
        int cuotasMora = (plan.getCuotasAtrasadas() != null) ? plan.getCuotasAtrasadas() : 0;

        summaryPara.add(new Chunk("CUOTAS TOTALES: ", boldFont));
        summaryPara.add(new Chunk(String.valueOf(totalCuotas) + "\n", regularFont));
        summaryPara.add(new Chunk("CUOTAS PAGADAS: ", boldFont));
        summaryPara.add(new Chunk(String.valueOf(cuotasPagadas) + "\n", regularFont));
        summaryPara.add(new Chunk("CUOTAS RESTANTES: ", boldFont));
        summaryPara.add(new Chunk(String.valueOf(cuotasRestantes) + "\n", regularFont));
        
        if (cuotasMora > 0) {
            summaryPara.add(new Chunk("CUOTAS EN MORA: ", boldFont));
            summaryPara.add(new Chunk(cuotasMora + " (VENCIDO)\n", boldFont));
        } else {
            summaryPara.add(new Chunk("CUOTAS EN MORA: ", boldFont));
            summaryPara.add(new Chunk("0\n", regularFont));
        }

        summaryPara.add(new Chunk("================================\n", regularFont));
        document.add(summaryPara);

        // -------------------------------------------------------------
        // OBSERVATIONS (If any)
        // -------------------------------------------------------------
        if (payment.getObservaciones() != null && !payment.getObservaciones().trim().isEmpty()) {
            Paragraph obsPara = new Paragraph();
            obsPara.setLeading(9f);
            obsPara.add(new Chunk("OBS: ", boldFont));
            obsPara.add(new Chunk(payment.getObservaciones() + "\n", regularFont));
            obsPara.add(new Chunk("--------------------------------\n", regularFont));
            document.add(obsPara);
        }

        // -------------------------------------------------------------
        // FOOTER TERMS & SIGNATURE
        // -------------------------------------------------------------
        Paragraph footerPara = new Paragraph();
        footerPara.setAlignment(Element.ALIGN_CENTER);
        footerPara.setLeading(10f);
        
        // Small seal area
        footerPara.add(new Chunk("\n\n\n_______________________________\n", regularFont));
        footerPara.add(new Chunk("SELLO DEL CONCESIONARIO\n\n", boldFont));
        
        footerPara.add(new Chunk("Conserve esta tirilla como soporte.\n", miniFont));
        footerPara.add(new Chunk("Verifique que la información sea correcta.\n", miniFont));
        footerPara.add(new Chunk("¡Gracias por su pago!\n\n", boldFont));
        footerPara.add(new Chunk("*** COMERCIO ***", titleFont));
        document.add(footerPara);

        document.close();
        return out.toByteArray();
    }

    public LocalDate calculateNextPaymentDate(LocalDate start, int cuotasPagadas, String frequency) {
        if (start == null) return LocalDate.now(java.time.ZoneId.of("America/Bogota"));
        int count = cuotasPagadas + 1;
        if (frequency == null) return start.plusMonths(count);
        switch (frequency.toUpperCase()) {
            case "DIARIA":
                return start.plusDays(count);
            case "SEMANAL":
                return start.plusWeeks(count);
            case "QUINCENAL":
                return start.plusWeeks(count * 2L);
            case "MENSUAL":
            default:
                return start.plusMonths(count);
        }
    }
}
