
package com.crio.warmup.stock;


import com.crio.warmup.stock.dto.*;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;  
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.apache.logging.log4j.ThreadContext;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;


public class PortfolioManagerApplication {
  @Autowired
  static
  RestTemplate restTemplate;

  // TODO: CRIO_TASK_MODULE_JSON_PARSING
  //  Task:
  //       - Read the json file provided in the argument[0], The file is available in the classpath.
  //       - Go through all of the trades in the given file,
  //       - Prepare the list of all symbols a portfolio has.
  //       - if "trades.json" has trades like
  //         [{ "symbol": "MSFT"}, { "symbol": "AAPL"}, { "symbol": "GOOGL"}]
  //         Then you should return ["MSFT", "AAPL", "GOOGL"]
  //  Hints:
  //    1. Go through two functions provided - #resolveFileFromResources() and #getObjectMapper
  //       Check if they are of any help to you.
  //    2. Return the list of all symbols in the same order as provided in json.

  //  Note:
  //  1. There can be few unused imports, you will need to fix them to make the build pass.
  //  2. You can use "./gradlew build" to check if your code builds successfully.

  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {
    String file = args[0];
    String contents = readFileAsString(file);
    
    ObjectMapper objMapper = getObjectMapper();
    PortfolioTrade[] tradeArray = objMapper.readValue(contents, PortfolioTrade[].class);
    List<String> list = new ArrayList<String>();

    for(int i = 0;i<tradeArray.length ; i++){
      list.add(tradeArray[i].getSymbol());
    }
      return list; 
  }
  
      private static String readFileAsString(String file) throws IOException, URISyntaxException{
        return new String(Files.readAllBytes(resolveFileFromResources(file).toPath()));
      //Path fileName= Path.of("C:\\Users\\HP\\Desktop\\gfg.txt");
      //String str = Files.readString(fileName);
      //return str;
    
    }
    
    public static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token) {
      String url = prepareUrl(trade, endDate, token);
      RestTemplate restTemplate = new RestTemplate();
      Candle[] candles = restTemplate.getForObject(url, TiingoCandle[].class);
      return Arrays.asList(candles);
      
    }    

    public static Double getOpeningPriceOnStartDate(List<Candle> candles) {
      return candles.get(0).getOpen();
    }

    public static Double getClosingPriceOnEndDate(List<Candle> candles) {
      return candles.get(candles.size() - 1).getClose();
    }
    
  // TODO: CRIO_TASK_MODULE_CALCULATIONS
  //  Now that you have the list of PortfolioTrade and their data, calculate annualized returns
  //  for the stocks provided in the Json.
  //  Use the function you just wrote #calculateAnnualizedReturns.
  //  Return the list of AnnualizedReturns sorted by annualizedReturns in descending order.

  // Note:
  // 1. You may need to copy relevant code from #mainReadQuotes to parse the Json.
  // 2. Remember to get the latest quotes from Tiingo API.

  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] strings) throws IOException, URISyntaxException {
    List<PortfolioTrade> portfolioTradesList = readTradesFromJson(strings[0]);
    LocalDate endDate = LocalDate.parse(strings[1]);

    return portfolioTradesList.stream().map(trade ->{
      List<Candle> candles = fetchCandles(trade, LocalDate.parse(strings[1]), getToken());

      double openPrice = candles.get(0).getOpen();
      double closePrice = candles.get(candles.size()-1).getClose();
      AnnualizedReturn annualizedReturns = calculateAnnualizedReturns(endDate, trade, openPrice, closePrice);
      return annualizedReturns;
    }).sorted(Comparator.comparing(AnnualizedReturn :: getAnnualizedReturn).reversed()).collect(Collectors.toList());
      
  }

   // TODO: CRIO_TASK_MODULE_CALCULATIONS
  //  Return the populated list of AnnualizedReturn for all stocks.
  //  Annualized returns should be calculated in two steps:
  //   1. Calculate totalReturn = (sell_value - buy_value) / buy_value.
  //      1.1 Store the same as totalReturns
  //   2. Calculate extrapolated annualized returns by scaling the same in years span.
  //      The formula is:
  //      annualized_returns = (1 + total_returns) ^ (1 / total_num_years) - 1
  //      2.1 Store the same as annualized_returns
  //  Test the same using below specified command. The build should be successful.
  //     ./gradlew test --tests PortfolioManagerApplicationTest.testCalculateAnnualizedReturn

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
  PortfolioTrade trade, Double buyPrice, Double sellPrice) {
    double totalReturn =  (sellPrice - buyPrice) / buyPrice ;
    double numYears = ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate) / 365.24; 
    double annualizedReturns = Math.pow((1 + totalReturn), (1 / numYears) ) - 1 ;
    return new AnnualizedReturn(trade.getSymbol(), annualizedReturns, totalReturn);
  }

      public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {
        List<PortfolioTrade> portfolioTradesList = readTradesFromJson(args[0]);
        List<TotalReturnsDto> totalReturnsDtosList = new ArrayList<>(); 
        
        for (int i = 0; i < portfolioTradesList.size(); i++) {
          List<Candle> candles = fetchCandles(portfolioTradesList.get(i), LocalDate.parse(args[1]), getToken());
          Double closingPrice=getClosingPriceOnEndDate(candles);
          totalReturnsDtosList.add(new TotalReturnsDto(portfolioTradesList.get(i).getSymbol(), closingPrice));
        }
    
        Collections.sort(totalReturnsDtosList, new Comparator<TotalReturnsDto>() {
    
          @Override
          public int compare(TotalReturnsDto p1, TotalReturnsDto p2) {
            return p1.getClosingPrice() > p2.getClosingPrice() ? 1 : -1;
          }
    
        });
    
        List<String> ans = new ArrayList<>();
        for (TotalReturnsDto t : totalReturnsDtosList) {
          ans.add(t.getSymbol());
        }
    
        return ans;
    
      }

      static String getToken() {
        // return "affec81a53d959bf6d0517b0d33730d4d4109e11"; 
        return "d04e9fd7d5c3a37ed535c1c0b8ec095e7d1214fa";
      }
    
      public static ArrayList<TotalReturnsDto> getTotalReturnDtoList(String[] args, List<PortfolioTrade> portfolioTradesList){

          RestTemplate template = new RestTemplate();
          ArrayList<TotalReturnsDto> totalReturnsDtosList = new ArrayList<TotalReturnsDto>();
          String token = "6818e5feeab6f206f84c25830503f71bd94d398a";
         for(PortfolioTrade trade : portfolioTradesList){
          String url = prepareUrl( trade, trade.getPurchaseDate(), token );
          TiingoCandle[] tiingoCandlesList = template.getForObject(url, TiingoCandle[].class);
          if( tiingoCandlesList != null ){
            totalReturnsDtosList.add( new TotalReturnsDto(trade.getSymbol(), tiingoCandlesList[tiingoCandlesList.length - 1].getClose()) );
          }
         }
        return totalReturnsDtosList;
      }

      public static ArrayList<PortfolioTrade> readTradesFromJson(String filename) throws IOException, URISyntaxException {
    
        String jsonString =  new String(Files.readAllBytes(resolveFileFromResources(filename).toPath()));
        ObjectMapper mapper = getObjectMapper();
        PortfolioTrade[] portfolioTrades = mapper.readValue(jsonString, PortfolioTrade[].class);
        ArrayList<PortfolioTrade> portfolioTradesList = new ArrayList<PortfolioTrade>();
    
        for(PortfolioTrade port : portfolioTrades){
          portfolioTradesList.add(port);
        }
        return portfolioTradesList;
      }
  // Note:
  // 1. You may need to copy relevant code from #mainReadQuotes to parse the Json.
  // 2. Remember to get the latest quotes from Tiingo API.

  // TODO: CRIO_TASK_MODULE_REST_API
  //  Find out the closing price of each stock on the end_date and return the list
  //  of all symbols in ascending order by its close value on end date.

  // Note:
  // 1. You may have to register on Tiingo to get the api_token.
  // 2. Look at args parameter and the module instructions carefully.
  // 2. You can copy relevant code from #mainReadFile to parse the Json.
  // 3. Use RestTemplate#getForObject in order to call the API,
  //    and deserialize the results in List<Candle>



  private static void printJsonObject(Object object) throws IOException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }

  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(
        Thread.currentThread().getContextClassLoader().getResource(filename).toURI()).toFile();
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }


  // TODO: CRIO_TASK_MODULE_JSON_PARSING
  //  Follow the instructions provided in the task documentation and fill up the correct values for
  //  the variables provided. First value is provided for your reference.
  //  A. Put a breakpoint on the first line inside mainReadFile() which says
  //    return Collections.emptyList();
  //  B. Then Debug the test #mainReadFile provided in PortfoliomanagerApplicationTest.java
  //  following the instructions to run the test.
  //  Once you are able to run the test, perform following tasks and record the output as a
  //  String in the function below.
  //  Use this link to see how to evaluate expressions -
  //  https://code.visualstudio.com/docs/editor/debugging#_data-inspection
  //  1. evaluate the value of "args[0]" and set the value
  //     to the variable named valueOfArgument0 (This is implemented for your reference.)
  //  2. In the same window, evaluate the value of expression below and set it
  //  to resultOfResolveFilePathArgs0
  //     expression ==> resolveFileFromResources(args[0])
  //  3. In the same window, evaluate the value of expression below and set it
  //  to toStringOfObjectMapper.
  //  You might see some garbage numbers in the output. Dont worry, its expected.
  //    expression ==> getObjectMapper().toString()
  //  4. Now Go to the debug window and open stack trace. Put the name of the function you see at
  //  second place from top to variable functionNameFromTestFileInStackTrace
  //  5. In the same window, you will see the line number of the function in the stack trace window.
  //  assign the same to lineNumberFromTestFileInStackTrace
  //  Once you are done with above, just run the corresponding test and
  //  make sure its working as expected. use below command to do the same.
  //  ./gradlew test --tests PortfolioManagerApplicationTest.testDebugValues

  public static List<String> debugOutputs() {

     String valueOfArgument0 = "trades.json";
     String resultOfResolveFilePathArgs0 = "trades.json";
     String toStringOfObjectMapper = "ObjectMapper";
     String functionNameFromTestFileInStackTrace = "mainReadFile";
     String lineNumberFromTestFileInStackTrace = "";


    return Arrays.asList(new String[]{valueOfArgument0, resultOfResolveFilePathArgs0,
        toStringOfObjectMapper, functionNameFromTestFileInStackTrace,
        lineNumberFromTestFileInStackTrace});
  }


  // Note:
  // Remember to confirm that you are getting same results for annualized returns as in Module 3.
 

  // TODO:
  //  After refactor, make sure that the tests pass by using these two commands
  //  ./gradlew test --tests PortfolioManagerApplicationTest.readTradesFromJson
  //  ./gradlew test --tests PortfolioManagerApplicationTest.mainReadFile
  


  // TODO:
  //  Build the Url using given parameters and use this function in your code to cann the API.
  public static String prepareUrl(PortfolioTrade trade, LocalDate endDate, String token) {
      return "https://api.tiingo.com/tiingo/daily/"+trade.getSymbol()+"/prices?startDate="+trade.getPurchaseDate().toString()+"&endDate="+endDate+"&token="+token;

  }

  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Once you are done with the implementation inside PortfolioManagerImpl and
  //  PortfolioManagerFactory, create PortfolioManager using PortfolioManagerFactory.
  //  Refer to the code from previous modules to get the List<PortfolioTrades> and endDate, and
  //  call the newly implemented method in PortfolioManager to calculate the annualized returns.

  // Note:
  // Remember to confirm that you are getting same results for annualized returns as in Module 3.

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
      throws Exception {
       PortfolioManager portfolioManager =  PortfolioManagerFactory.getPortfolioManager(restTemplate);
       String file = args[0];
       LocalDate endDate = LocalDate.parse(args[1]);
       String contents = readFileAsString(file);
       ObjectMapper objectMapper = getObjectMapper();
       PortfolioTrade[] portfolioTrades = objectMapper.readValue(contents, PortfolioTrade[].class);
       return portfolioManager.calculateAnnualizedReturn(Arrays.asList(portfolioTrades), endDate);
  }


  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());

    printJsonObject(mainReadFile(args));


    printJsonObject(mainReadQuotes(args));
    printJsonObject(mainCalculateReturnsAfterRefactor(args));

      }   
  }


