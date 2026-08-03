package engine.incubator.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("specification")
final class NoMutableAssetGlobalTest {
    @Test
    void coreAssetContractsDeclareNoMutableStaticState() {
        List<Class<?>> contractTypes = List.of(
            AssetDiagnostic.class,
            AssetEntry.class,
            AssetException.class,
            AssetHandle.class,
            AssetGroupHandle.class,
            AssetId.class,
            AssetLoad.class,
            AssetManifest.class,
            AssetMetrics.class,
            AssetProgress.class,
            AssetService.class,
            SharedAssetData.class
        );

        List<String> mutableStatics = contractTypes.stream()
            .flatMap(type -> List.of(type.getDeclaredFields()).stream())
            .filter(field -> Modifier.isStatic(field.getModifiers()))
            .filter(field -> !Modifier.isFinal(field.getModifiers()))
            .map(field -> field.getDeclaringClass().getName() + "#" + field.getName())
            .sorted()
            .toList();

        assertEquals(List.of(), mutableStatics);
    }
}
