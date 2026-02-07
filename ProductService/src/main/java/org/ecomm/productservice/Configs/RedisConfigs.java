package org.ecomm.productservice.Configs;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.*;


import java.time.Duration;

import static com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL;

@Configuration
@EnableCaching
public class RedisConfigs {

    @Bean
   public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {

       ObjectMapper objectMappe = new ObjectMapper();
       objectMappe.registerModule(new JavaTimeModule());
       objectMappe.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
       objectMappe.activateDefaultTyping(
               LaissezFaireSubTypeValidator.instance,
               NON_FINAL,
               JsonTypeInfo.As.PROPERTY
       );

       RedisSerializer<Object> serializer = new RedisSerializer<>() {

           @Override
           public byte[] serialize(Object value) throws SerializationException {
               if (value == null) {
                   return new byte[0];
               }
               try {
                   return objectMappe.writeValueAsBytes(value);
               } catch (Exception e) {
                   throw new SerializationException("Could not serialize: " + e.getMessage(), e);
               }
           }

           @Override
           public Object deserialize(byte[] bytes) throws SerializationException {
               if (bytes == null || bytes.length == 0) {
                   return null;
               }
               try {
                   return objectMappe.readValue(bytes, Object.class);
               } catch (Exception e) {
                   throw new SerializationException("Could not deserialize: " + e.getMessage(), e);
               }
           }
       };

       RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
               .entryTtl(Duration.ofMinutes(10))
               .disableCachingNullValues()
               .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string()))
               .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

       return RedisCacheManager.builder(redisConnectionFactory).cacheDefaults(config).build();
   }
}
