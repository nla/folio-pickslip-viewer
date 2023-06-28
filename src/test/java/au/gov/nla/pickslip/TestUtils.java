package au.gov.nla.pickslip;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;

public class TestUtils {

    private static ResourceLoader resourceLoader = new DefaultResourceLoader();
    private static ObjectMapper mapper = new ObjectMapper();

    public static JsonNode loadJson(String filename) throws IOException {
        Resource r = resourceLoader.getResource("classpath:json/" + filename);
        return mapper.readTree(r.getFile());
    }

}
