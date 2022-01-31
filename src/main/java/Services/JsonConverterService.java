package Services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;

public class JsonConverterService {

    public <T> T deserializeWithJackson(String jsonStrg, Class<T> type) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            T output = mapper.readValue(jsonStrg, type);
            return output;
        }
        catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    public <T> void serializeWithJackson(String filePath, T inputObject) {
        Gson gson = new GsonBuilder().create();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(new File(filePath), inputObject);
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
}
