package com.level.networth.model;

import java.util.List;

public class Customer {

    public String userId;
    public String name;
    public Metadata metadata;
    public List<PortfolioItem> portfolio;

    public static class Metadata {
        public String region;
        public String segment;
    }
}
