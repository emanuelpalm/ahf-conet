package eu.arrowhead.proto.cosys.datasharing;

import eu.arrowhead.common.CommonConstants;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {CommonConstants.BASE_PACKAGE, DataConsumerConstants.BASE_PACKAGE})
public class DataConsumerMain {
    public static void main(final String[] args) {
        SpringApplication.run(DataConsumerMain.class, args);

    }

}
