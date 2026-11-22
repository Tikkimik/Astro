package com.gdx.scenarios;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class ScenarioCatalogMain {

    private ScenarioCatalogMain() {}

    public static void main(String[] args) {
        List<String> tags = args.length == 0 || args[0].isBlank()
                ? List.of()
                : Arrays.stream(args[0].split(","))
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .toList();
        Collection<ScenarioDefinition> scenarios = tags.isEmpty()
                ? ScenarioRegistry.all()
                : ScenarioRegistry.withTags(tags);
        for (ScenarioDefinition scenario : scenarios) {
            System.out.println(scenario.name());
        }
    }
}
