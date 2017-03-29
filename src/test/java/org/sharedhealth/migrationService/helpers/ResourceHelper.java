package org.sharedhealth.migrationService.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ResourceHelper {
    public static String asString(String fileName) throws IOException {
        InputStream resourceAsStream = ResourceHelper.class.getClassLoader().getResourceAsStream(fileName);
        if (resourceAsStream != null) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourceAsStream));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                response.append(line);
            }
            bufferedReader.close();
            return response.toString();
        }
        return null;
    }
}
