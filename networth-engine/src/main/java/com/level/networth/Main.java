package com.level.networth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.level.networth.model.Customer;
import com.level.networth.service.NetWorthCalculator;

import java.io.File;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        List<Customer> customers = mapper.readValue(
              //  new File("data/customers.json"),
	        Main.class.getClassLoader().getResourceAsStream("customers.json"),
                new TypeReference<List<Customer>>() {}
        );

        NetWorthCalculator calculator = new NetWorthCalculator();

        for (Customer customer : customers) {
            calculator.calculate(customer);
        }
    }
}
