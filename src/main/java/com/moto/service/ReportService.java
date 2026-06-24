package com.moto.service;

import com.moto.model.FinancingPlan;
import com.moto.model.Motorcycle;
import com.moto.model.Payment;
import com.moto.repository.MotorcycleRepository;
import com.moto.controller.PaymentController;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportService {

    @Autowired
    private MotorcycleRepository motorcycleRepository;

    public byte[] exportPaymentsToExcel(List<PaymentController.PaymentWithDetails> payments) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Libro de Caja - Recaudos");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        // Styling for header
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        // Header Row
        String[] headers = {"ID Recibo", "Fecha", "Comprador", "Placa Vehículo", "Cuota N°", "Monto Abonado", "Método Pago", "Registrado Por", "Observaciones"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Currency format helper
        CellStyle currencyStyle = workbook.createCellStyle();
        currencyStyle.setDataFormat((short) 8); // Format: $#,##0

        // Populating Data
        int rowIdx = 1;
        for (PaymentController.PaymentWithDetails pwd : payments) {
            Row row = sheet.createRow(rowIdx++);
            Payment p = pwd.getPayment();
            
            row.createCell(0).setCellValue(p.getId());
            row.createCell(1).setCellValue(p.getFechaPago().format(dtf));
            row.createCell(2).setCellValue(pwd.getBuyerName());
            row.createCell(3).setCellValue(pwd.getPlate());
            
            row.createCell(4).setCellValue(p.getNumeroCuota() == 0 ? "Cuota Inicial" : String.valueOf(p.getNumeroCuota()));
            
            Cell valCell = row.createCell(5);
            valCell.setCellValue(p.getValorPagado());
            valCell.setCellStyle(currencyStyle);

            row.createCell(6).setCellValue(p.getMetodoPago());
            row.createCell(7).setCellValue(p.getRegistradoPor());
            row.createCell(8).setCellValue(p.getObservaciones() != null ? p.getObservaciones() : "");
        }

        // Auto-sizing columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    public byte[] exportFinancingPlansToExcel(List<FinancingPlan> plans) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Cartera de Créditos");

        // Styling for header
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        // Header Row
        String[] headers = {
            "ID Crédito", "Comprador", "Cédula", "Teléfono", "Vehículo", "Placa", 
            "Valor Moto", "Cuota Inicial", "Saldo Financiado", "Saldo Pendiente", 
            "Valor de Cuota", "Cuotas Pagadas", "Cuotas Totales", "Frecuencia",
            "Estado Crédito", "Cuotas Vencidas", "Días de Retraso", "Monto en Mora"
        };
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Currency format style
        CellStyle currencyStyle = workbook.createCellStyle();
        currencyStyle.setDataFormat((short) 8); // Format: $#,##0

        int rowIdx = 1;
        for (FinancingPlan plan : plans) {
            Row row = sheet.createRow(rowIdx++);

            // Resolve Motorcycle info (fallback if deleted)
            String motoName = "Vehículo Eliminado";
            String plate = "-";
            Motorcycle m = motorcycleRepository.findById(plan.getMotorcycleId()).orElse(null);
            if (m != null) {
                motoName = m.getMarca() + " " + m.getModelo();
                plate = m.getPlaca();
            }

            row.createCell(0).setCellValue(plan.getId());
            row.createCell(1).setCellValue(plan.getBuyer().getNombreCompleto());
            row.createCell(2).setCellValue(plan.getBuyer().getCedula());
            row.createCell(3).setCellValue(plan.getBuyer().getTelefono());
            row.createCell(4).setCellValue(motoName);
            row.createCell(5).setCellValue(plate);

            Cell c6 = row.createCell(6); c6.setCellValue(plan.getValorTotal()); c6.setCellStyle(currencyStyle);
            Cell c7 = row.createCell(7); c7.setCellValue(plan.getCuotaInicial()); c7.setCellStyle(currencyStyle);
            Cell c8 = row.createCell(8); c8.setCellValue(plan.getSaldoFinanciado()); c8.setCellStyle(currencyStyle);
            Cell c9 = row.createCell(9); c9.setCellValue(plan.getSaldoPendiente()); c9.setCellStyle(currencyStyle);
            Cell c10 = row.createCell(10); c10.setCellValue(plan.getValorCuota()); c10.setCellStyle(currencyStyle);

            row.createCell(11).setCellValue(plan.getCuotasPagadas());
            row.createCell(12).setCellValue(plan.getCuotasTotales());
            row.createCell(13).setCellValue(plan.getFrecuenciaPago());
            row.createCell(14).setCellValue(plan.getEstadoCredito());
            
            row.createCell(15).setCellValue(plan.getCuotasAtrasadas() != null ? plan.getCuotasAtrasadas() : 0);
            row.createCell(16).setCellValue(plan.getDiasRetraso() != null ? plan.getDiasRetraso() : 0L);
            
            Cell c17 = row.createCell(17); 
            c17.setCellValue(plan.getValorTotalAdeudado() != null ? plan.getValorTotalAdeudado() : 0.0); 
            c17.setCellStyle(currencyStyle);
        }

        // Auto-sizing columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }
}
