package eu.arrowhead.proto.cosys;

import eu.arrowhead.common.CommonConstants;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {CommonConstants.BASE_PACKAGE, ContractSystemConstants.BASE_PACKAGE})
public class ContractSystemMain {
   public static void main(final String[] args) {
      SpringApplication.run(ContractSystemMain.class, args);
   }
}
