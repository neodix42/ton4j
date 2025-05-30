package org.ton.ton4j.address;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.util.Map;

@Builder
@Data
public class TonSdkTestCases implements Serializable {
    private Map<String, TestCase> testCases;

    @Setter
    @Getter
    public static class TestCase {

        private String description;
        private Map<String, Object> input;
        private Map<String, Object> expectedOutput;

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }
}
