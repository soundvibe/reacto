package net.soundvibe.reacto.types;

import com.google.protobuf.ByteString;
import net.soundvibe.reacto.internal.ObjectId;
import net.soundvibe.reacto.internal.Messages;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static net.soundvibe.reacto.internal.Messages.Command.newBuilder;

/**
 * @author OZY on 2016.02.05.
 */
public class MessagesTest {

    @Test
    public void shouldPersistCommand() throws Exception {

        final Messages.Metadata.Builder metaDataBuilder = Messages.Metadata.newBuilder();
        Map<String, String> meta = new LinkedHashMap<>(2);
        meta.put("foo", "bar");
        meta.put("foo2", "bar2");

        final List<Messages.Metadata> metaDataList = meta.entrySet().stream()
                .map(entry -> metaDataBuilder.setKey(entry.getKey()).setValue(entry.getValue()).build())
                .collect(Collectors.toList());

        final Messages.Command expected = Messages.Command.newBuilder()
                .setId(ObjectId.get().toString())
                .setName("DoSomething")
                .setPayload(ByteString.copyFrom("Some payload".getBytes()))
                .addAllMetadata(metaDataList)
                .build();

        final byte[] bytes = expected.toByteArray();
        final String payload = expected.toByteString().toStringUtf8();
        System.out.println(payload);

        final Messages.Command actual = Messages.Command.parseFrom(bytes);
        assertEquals(expected, actual);

        assertEquals(2, actual.getMetadataCount());
        assertEquals("foo", actual.getMetadata(0).getKey());
        assertEquals("bar", actual.getMetadata(0).getValue());
        assertEquals("foo2", actual.getMetadata(1).getKey());
        assertEquals("bar2", actual.getMetadata(1).getValue());
        assertEquals("DoSomething", actual.getName());
        assertEquals("Some payload", actual.getPayload().toStringUtf8());
    }
}
