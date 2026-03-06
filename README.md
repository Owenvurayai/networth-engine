Net Worth Calculation Engine

A Java-based command-line engine that calculates the total net worth in GBP for a list of clients. It integrates three data sources — a local JSON file, a live stock/crypto pricing API, and a live currency exchange rate API — and produces a financial report per customer.




Tech Stack

The project is built with Java 17 and managed using Maven. Jackson is used for JSON parsing and local cache serialisation. All monetary calculations are handled using BigDecimal to prevent floating point precision loss — a critical requirement in any financial system. Live stock and crypto prices are sourced from the Finnhub API, and currency conversion rates are sourced from the ExchangeRate API.




Architecture

The application follows a service-based architecture where each class owns a single responsibility. Main loads customer data from customers.json and triggers the calculation. NetWorthCalculator orchestrates the full calculation per customer, aggregating assets and liabilities into a final net worth figure. PricingService fetches live prices from Finnhub and falls back to cached_prices.json if the API is unavailable. ExchangeRateService fetches live GBP conversion rates and falls back to cached_fx_rates.json if needed. MoneyUtils is a utility class that centralises all BigDecimal arithmetic. PortfolioItem, Customer, and AssetType are the model classes representing the customer data structure.





Prerequisites

Java 17+

Maven 3.8+

A free Finnhub API key → https://finnhub.io

A free ExchangeRate API key → https://www.exchangerate-api.com




API Key Setup

This project reads API keys from environment variables so no credentials are hardcoded in the source.


Step 1 — Obtain your API keys

For Finnhub, go to https://finnhub.io, create a free account, and your API key will be displayed on the dashboard immediately after sign up.

For ExchangeRate API, go to https://www.exchangerate-api.com, create a free account, and your key will be available on your account dashboard.


Step 2 — Set your environment variables


Option A — Current session only (temporary):

bashexport FINNHUB_KEY=your_finnhub_key_here

export EXCHANGE_API_KEY=your_exchangerate_key_here

Verify they are set:

bashecho $FINNHUB_KEY

echo $EXCHANGE_API_KEY


Option B — Permanent (recommended):

bashecho 'export FINNHUB_KEY=your_finnhub_key_here' >> ~/.bashrc

echo 'export EXCHANGE_API_KEY=your_exchangerate_key_here' >> ~/.bashrc
source ~/.bashrc




Running the Project

bash  

mvn compile

mvn exec:java -Dexec.mainClass="com.level.networth.Main"




Sample Output

----------------------------------

User: Sarah Chen

Total Assets:      £1163233.36

Total Liabilities:  £314286.00

Net Worth:         £848947.36

Status: Final

----------------------------------


If the live API is unreachable, the engine automatically falls back to the local cache and flags the report:

Status: Estimated




Resiliency

Every successful API call writes the latest price or rate to a local cache file. If the live API is unavailable, the engine reads from cache and marks the report as Estimated. If both the live API and cache are unavailable, the calculation aborts cleanly rather than producing an incorrect financial result.



Author

Owen Vurayai
