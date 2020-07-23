package com.celements.marshalling;

import static com.google.common.base.Preconditions.*;

import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Optional;

@Immutable
public class EnumMarshaller<E extends Enum<E>> extends AbstractMarshaller<E> {

  private final Function<E, String> serializer;

  public EnumMarshaller(Class<E> token) {
    this(token, Enum::name);
  }

  public EnumMarshaller(Class<E> token, Function<E, String> serializer) {
    super(token);
    this.serializer = checkNotNull(serializer);
  }

  @Override
  public String serialize(E val) {
    return serializer.apply(val);
  }

  @Override
  public Optional<E> resolve(String val) {
    return Optional.fromJavaUtil(Stream.of(getToken().getEnumConstants())
        .filter(enm -> serializer.apply(enm).equals(val))
        .findAny());
  }

}
