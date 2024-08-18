package com.glenneligio.service;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.glenneligio.model.PropertiesFileEntry;
import com.glenneligio.model.YamlFileEnvEntry;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class YamlServiceImplTest {

    private static final String VALID_YAML_FILE_NAME = "src/test/resources/deployment-test.yml";
    private static final String VALID_YAML_WITH_NO_CONTENT_FILE_NAME = "src/test/resources/deployment-no-content.yml";
    private static final String NON_EXISTENT_FILE = "src/test/resources/deployment-non-existent.yml";
    private static final String DIRECTORY_NAME = "src/test/resources/deployment-folder.yml";
    private static final String NON_READABLE_YAML_FILE_NAME = "src/test/resources/deployment-non-readable.yml";
    private static final String NON_CONTAINER_FIELD = "src/test/resources/deployment-no-container.yml";
    private static final String NON_MATCHING_CONTAINER_ITEM = "src/test/resources/deployment-no-matching-container.yml";
    private static final String CONTAINER_IMAGE = "client-service";
    private YamlFileEnvEntry y0, y1, y2, y3, y4;
    private List<YamlFileEnvEntry> validYamlEnvFileEntries = new ArrayList<>();

    @BeforeEach
    void setupEach() {
        populateValidYamlEnvFileEntries();
    }

    private void populateValidYamlEnvFileEntries() {
        y0 = new YamlFileEnvEntry("DB_HOST", "localhost", false);
        y1 = new YamlFileEnvEntry("DB_PORT", "9090", false);
        y2 = new YamlFileEnvEntry("JUST_AN_ENV", "justAnEnvValue", false);
        y3 = new YamlFileEnvEntry("SECRET_API_KEY", null, true);
        y4 = new YamlFileEnvEntry("ENV_SECRET_ONLY_IN_YAML", null, true);
        validYamlEnvFileEntries = new ArrayList<>(Lists.list(y0, y1, y2, y3, y4));
    }

    // getYamlFileEnvEntries
    // from 1 to 5, throws Exception
    // 1. if the yaml file does not exist
    @Test
    void givenYamlFileDoesNotExist_WhenGetYamlFileEnvEntriesIsCalled_throwException() {
        YamlService yamlService = new YamlServiceImpl();
        Assertions.assertThrows(FileNotFoundException.class, () -> yamlService.getYamlFileEnvEntries(NON_EXISTENT_FILE, CONTAINER_IMAGE));
    }

    // 2. if the yaml file location is a directory
    @Test
    void givenYamlFileLocationIsADirectory_WhenGetYamlFileEnvEntriesIsCalled_throwException() {
        YamlService yamlService = new YamlServiceImpl();
        Assertions.assertThrows(RuntimeException.class, () -> yamlService.getYamlFileEnvEntries(DIRECTORY_NAME, CONTAINER_IMAGE));
    }

    // 3. if the yaml file location is not readable
    // non-testable via unit test, needs to modify the permissions manually
    // will fail
    @Test
    @Disabled
    void givenYamlFileLocationIsNotReadable_WhenGetYamlFileEnvEntriesIsCalled_throwException() throws IOException {
        YamlService yamlService = new YamlServiceImpl();
        Assertions.assertThrows(AccessDeniedException.class, () -> yamlService.getYamlFileEnvEntries(NON_READABLE_YAML_FILE_NAME, CONTAINER_IMAGE));
    }

    // 4. if the yaml file have no 'containers' arrayNode
    @Test
    void givenYamlContentHasNoContainersField_WhenGetYamlFileEnvEntriesIsCalled_throwException() {
        YamlService yamlService = new YamlServiceImpl();
        Assertions.assertThrows(RuntimeException.class, () -> yamlService.getYamlFileEnvEntries(NON_CONTAINER_FIELD, CONTAINER_IMAGE));
    }

    // 5. if the yaml file have no item in 'containers' arrayNode that matches the imageName input
    @Test
    void givenYamlFileContainersArrayDoNotHaveTheImageInput_WhenGetYamlFileEnvEntriesIsCalled_throwException() {
        YamlService yamlService = new YamlServiceImpl();
        Assertions.assertThrows(RuntimeException.class, () -> yamlService.getYamlFileEnvEntries(NON_MATCHING_CONTAINER_ITEM, CONTAINER_IMAGE));
    }

    // 6. if the yaml file no env field - returns empty array
    @Test
    void givenYamlFileEnvFieldIsMissing_WhenGetYamlFileEnvEntriesIsCalled_returnEmptyList() throws AccessDeniedException, FileNotFoundException, JsonProcessingException {
        YamlService yamlService = new YamlServiceImpl();
        List<YamlFileEnvEntry> result = yamlService.getYamlFileEnvEntries(VALID_YAML_WITH_NO_CONTENT_FILE_NAME, CONTAINER_IMAGE);
        Assertions.assertTrue(result.isEmpty());
    }

    // 7. if the yaml file have env field - returns corresponding list
    @Test
    void givenYamlFileEnvFieldIsPresent_WhenGetYamlFileEnvEntriesIsCalled_returnCorrespondingList() throws AccessDeniedException, FileNotFoundException, JsonProcessingException {
        YamlService yamlService = new YamlServiceImpl();
        List<YamlFileEnvEntry> result = yamlService.getYamlFileEnvEntries(VALID_YAML_FILE_NAME, CONTAINER_IMAGE);
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertEquals(validYamlEnvFileEntries, result);
    }
}
