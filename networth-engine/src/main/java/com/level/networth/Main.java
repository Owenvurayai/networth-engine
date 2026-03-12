package com.level.networth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.level.networth.model.Customer;
import com.level.networth.service.NetWorthCalculator;
import com.level.networth.service.ReportService;

import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        List<Customer> customers = mapper.readValue(
                Main.class.getClassLoader().getResourceAsStream("customers.json"),
                new TypeReference<List<Customer>>() {}
        );

        NetWorthCalculator calculator = new NetWorthCalculator();
        ReportService reportService = new ReportService();

        for (Customer customer : customers) {
            calculator.calculate(customer);   // prints to console
            reportService.writeReport(customer); // writes to file
        }

        System.out.println("Report saved to: " + reportService.getReportFilePath());
    }
}
