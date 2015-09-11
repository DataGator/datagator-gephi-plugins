package org.datagator.api.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Reader;

/**
 * Client-side data binding of DataGator entities.
 *
 * @author LIU Yu <liuyu@opencps.net>
 * @date 2015/09/01
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "kind")
@JsonSubTypes({
    @Type(name = "datagator#Matrix", value = SimpleMatrix.class)
})
public abstract class Entity
{

    protected static final JsonFactory json;
    
    static {
        json = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        json.setCodec(mapper);
    }

    public final String kind;

    @JsonCreator
    protected Entity(@JsonProperty("kind") String kind)
    {
        if (!kind.startsWith("datagator#")) {
            throw new RuntimeException(
                String.format("Invalid entity kind '%s'", kind));
        }
        this.kind = kind.substring("datagator#".length());
    }

    public static Entity create(Reader reader)
        throws IOException
    {
        JsonParser parser = json.createParser(reader);
        return parser.readValueAs(Entity.class);
    }
}
