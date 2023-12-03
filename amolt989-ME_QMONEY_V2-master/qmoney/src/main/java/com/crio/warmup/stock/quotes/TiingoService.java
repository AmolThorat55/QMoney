
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.client.RestTemplate;

//Reference - https:www.baeldung.com/jackson-ignore-properties-on-serialization
//Reference - https:www.baeldung.com/jackson-ignore-properties-on-serialization
public class TiingoService implements StockQuotesService {

  
  private RestTemplate restTemplate = new RestTemplate();
  protected TiingoService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }


  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Implement getStockQuote method below that was also declared in the interface.

  // Note:
  // 1. You can move the code from PortfolioManagerImpl#getStockQuote inside newly created method.
  // 2. Run the tests using command below and make sure it passes.
  //    ./gradlew test --tests TiingoServiceTest

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      {
        ObjectMapper objmapper=new ObjectMapper();
        objmapper.registerModule(new JavaTimeModule());

        Candle[] candleobj=null;

        String apiresponse=restTemplate.getForObject(buildUri(symbol, from, to), String.class);

        try {
          candleobj=objmapper.readValue(apiresponse, TiingoCandle[].class);
        } catch (JsonMappingException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (JsonProcessingException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        
        if(candleobj==null){
        
          return new ArrayList<>();
          
        }
  
        return Arrays.asList(candleobj);
        
  


      }


  protected static String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    String token = "d04e9fd7d5c3a37ed535c1c0b8ec095e7d1214fa";
    String uriTemplate = "https://api.tiingo.com/tiingo/daily/"+symbol+"/prices?"
    + "startDate="+startDate.toString()+"&endDate="+endDate.toString()+"&token="+token;
    return uriTemplate;
}
  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Write a method to create appropriate url to call the Tiingo API.

}
