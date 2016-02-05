package reactive.fp.types;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Cipolinas on 2015.11.16.
 */
public final class Command implements Message {

    public final ObjectId id;
    public final String name;
    public final Optional<MetaData> metaData;
    public final Optional<byte[]> payload;

    public Command(ObjectId id, String name, Optional<MetaData> metaData, Optional<byte[]> payload) {
        this.id = id;
        this.name = name;
        this.metaData = metaData;
        this.payload = payload;
    }

    public static Command create(String name, Optional<MetaData> metaData, Optional<byte[]> payload) {
        return new Command(ObjectId.get(), name, metaData, payload);
    }

    public static Command create(String name) {
        Objects.requireNonNull(name, "name cannot be null");
        return new Command(ObjectId.get(), name, Optional.empty(), Optional.empty());
    }

    public static Command create(String name, MetaData metaData) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(metaData, "metaData cannot be null");
        return new Command(ObjectId.get(), name, Optional.of(metaData), Optional.empty());
    }

    public static Command create(String name, MetaData metaData, byte[] payload) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(metaData, "metaData cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");
        return new Command(ObjectId.get(), name, Optional.of(metaData), Optional.of(payload));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Command command = (Command) o;
        return Objects.equals(id, command.id) &&
                Objects.equals(name, command.name) &&
                Objects.equals(metaData, command.metaData) &&
                Objects.equals(payload, command.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, metaData, payload);
    }

    @Override
    public String toString() {
        return "Command{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", metaData=" + metaData +
                ", payload=" + payload +
                '}';
    }
}
