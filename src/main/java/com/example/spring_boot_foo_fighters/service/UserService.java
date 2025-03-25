package com.example.spring_boot_foo_fighters.service;

import com.example.spring_boot_foo_fighters.dto.HumanDto;
import com.example.spring_boot_foo_fighters.dto.UserDto;
import com.example.spring_boot_foo_fighters.entity.HumanEntity;
import com.example.spring_boot_foo_fighters.entity.UserEntity;
import com.example.spring_boot_foo_fighters.exception.ErrorCode;
import com.example.spring_boot_foo_fighters.exception.ServiceException;
import com.example.spring_boot_foo_fighters.mapper.UserMapper;
//import com.example.spring_boot_foo_fighters.rabbitmq.RabbitMqMessageSender;
import com.example.spring_boot_foo_fighters.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String USERS_KEY = "users"; // Ключ для списка людей
    private static final String USER_KEY_PREFIX = "user:"; // Префикс для отдельных людей

    public UserEntity save(UserDto userDto) {
        if (userDto.getAge() < 6) {
            throw new ServiceException(ErrorCode.AGE_NOT_VALID);
        }
        if(userDto.getFirstName().length()>15){
            throw new ServiceException(ErrorCode.NAME_NOT_VALID, userDto.getFirstName());
        }

        UserEntity userEntity=userRepository.save(userMapper.toUserEntity(userDto));

        //add user to redis
        redisTemplate.opsForValue().set(USER_KEY_PREFIX + userEntity.getId(), userMapper.toUserDto(userEntity), 10, TimeUnit.MINUTES);
        return userEntity;
    }

    public UserDto getUserById(Long id) {
        String redisKey = USER_KEY_PREFIX + id;
        UserDto userDto = (UserDto) redisTemplate.opsForValue().get(redisKey);

        if (userDto != null) {
            log.info("Человек с ID {} найден в Redis", id);
            return userDto;
        }

        log.info("Человек с ID {} не найден в Redis, ищем в базе", id);
        UserEntity userEntity = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Human not found"));

        userDto = userMapper.toUserDto(userEntity);

        redisTemplate.opsForValue().set(redisKey, userDto, 10, TimeUnit.MINUTES);
        return userDto;
    }

}
