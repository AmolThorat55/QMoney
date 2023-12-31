
package com.crio.warmup.stock.portfolio;

import java.util.concurrent.ExecutionException;
import com.crio.warmup.stock.dto.AnnualizedReturn; 
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException; 
import static java.time.temporal.ChronoUnit.DAYS;
import com.crio.warmup.stock.quotes.StockQuotesService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {


RestTemplate restTemplate;
private StockQuotesService stockQuotesService; 

  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  protected PortfolioManagerImpl(StockQuotesService stockQuotesService) {
    this.stockQuotesService = stockQuotesService;
  }


  //TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from main anymore.
  //    Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
  //    into #calculateAnnualizedReturn function here and ensure it follows the method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required further as our
  //    clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command below:
  // ./gradlew test --tests PortfolioManagerTest

  //CHECKSTYLE:OFF

  private Comparator<AnnualizedReturn> getComparator() {
  return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  private Double getOpeningPriceOnStartDate(List<Candle> candles) {
    return candles.get(0).getOpen();
  }

  private Double getClosingPriceOnEndDate(List<Candle> candles) {
    return candles.get(candles.size() - 1).getClose();
  }

  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)throws JsonProcessingException, StockQuoteServiceException {
    return stockQuotesService.getStockQuote(symbol, from, to);
  }

  protected static String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
       String token = "d04e9fd7d5c3a37ed535c1c0b8ec095e7d1214fa";
       String uriTemplate = "https://api.tiingo.com/tiingo/daily/"+symbol+"/prices?"
       + "startDate="+startDate.toString()+"&endDate="+endDate.toString()+"&token="+token;
       return uriTemplate;
  }

  private AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate, PortfolioTrade trade,
      Double buyPrice, Double sellPrice) {
    double total_num_years = DAYS.between(trade.getPurchaseDate(), endDate) / 365.2422;
    double totalReturns = (sellPrice - buyPrice) / buyPrice;
    double annualized_returns = Math.pow((1.0 + totalReturns), (1.0 / total_num_years)) - 1;
    return new AnnualizedReturn(trade.getSymbol(), annualized_returns, totalReturns);
  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades, LocalDate endDate) throws StockQuoteServiceException, JsonProcessingException 
  {
    List<AnnualizedReturn> annualizedReturns = new ArrayList<>();

    for (PortfolioTrade portfolioTrade : portfolioTrades) {
      List<Candle> candles;
      candles = getStockQuote(portfolioTrade.getSymbol(), portfolioTrade.getPurchaseDate(), endDate);
      AnnualizedReturn annualizedReturn = calculateAnnualizedReturns(endDate, portfolioTrade,
        getOpeningPriceOnStartDate(candles), getClosingPriceOnEndDate(candles));
   annualizedReturns.add(annualizedReturn);
        
    }
    return annualizedReturns.stream().sorted(getComparator()).collect(Collectors.toList());
  }

  static String getToken() {
    return "d04e9fd7d5c3a37ed535c1c0b8ec095e7d1214fa"; 
  }

  public static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token) {
    String url = buildUri(trade.getSymbol(), trade.getPurchaseDate(), endDate);
    RestTemplate restTemplate = new RestTemplate();
    Candle[] candles = restTemplate.getForObject(url, TiingoCandle[].class);
    return Arrays.asList(candles);
    
  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturnParallel(
      List<PortfolioTrade> portfolioTrades, LocalDate endDate, int numThreads)
      throws InterruptedException, StockQuoteServiceException, JsonProcessingException {
        List<AnnualizedReturn> annualizedReturns = new ArrayList<AnnualizedReturn>();
        List<Future<AnnualizedReturn>> futureReturnsList = new ArrayList<Future<AnnualizedReturn>>(); 
        
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);

        for(int i= 0; i<portfolioTrades.size(); i++){
          PortfolioTrade trade = portfolioTrades.get(i); 
          Callable<AnnualizedReturn> callableTask = ()-> {
              List<Candle> candles = getStockQuote(trade.getSymbol(), trade.getPurchaseDate(), endDate);
              return calculateAnnualizedReturns(endDate, trade, getOpeningPriceOnStartDate(candles), 
              getClosingPriceOnEndDate(candles));
            };
            Future<AnnualizedReturn> futureReturns = pool.submit(callableTask);
            futureReturnsList.add(futureReturns);
    }

        for(Future<AnnualizedReturn> future : futureReturnsList){
          try{
            annualizedReturns.add(future.get());
          }catch(ExecutionException e){
                 throw new StockQuoteServiceException( e.getMessage());   
                       
        }
        Collections.sort(annualizedReturns, getComparator());
        pool.shutdown();
    }   
        return annualizedReturns; 
      }
    }


 


 
