package com.thoughtworks.rslist.service;

import com.thoughtworks.rslist.domain.RsEvent;
import com.thoughtworks.rslist.domain.Trade;
import com.thoughtworks.rslist.domain.Vote;
import com.thoughtworks.rslist.dto.RsEventDto;
import com.thoughtworks.rslist.dto.UserDto;
import com.thoughtworks.rslist.dto.VoteDto;
import com.thoughtworks.rslist.dto.TradeDto;
import com.thoughtworks.rslist.repository.RsEventRepository;
import com.thoughtworks.rslist.repository.TradeRepository;
import com.thoughtworks.rslist.repository.UserRepository;
import com.thoughtworks.rslist.repository.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class RsServiceTest {
  RsService rsService;

  @Mock RsEventRepository rsEventRepository;
  @Mock UserRepository userRepository;
  @Mock VoteRepository voteRepository;
  @Mock TradeRepository tradeRepository;
  LocalDateTime localDateTime;
  Vote vote;

  @BeforeEach
  void setUp() {
    initMocks(this);
    rsService = new RsService(rsEventRepository, userRepository, voteRepository, tradeRepository);
    localDateTime = LocalDateTime.now();
    vote = Vote.builder().voteNum(2).rsEventId(1).time(localDateTime).userId(1).build();
  }

  @Test
  void shouldVoteSuccess() {
    // given

    UserDto userDto =
        UserDto.builder()
            .voteNum(5)
            .phone("18888888888")
            .gender("female")
            .email("a@b.com")
            .age(19)
            .userName("xiaoli")
            .id(2)
            .build();
    RsEventDto rsEventDto =
        RsEventDto.builder()
            .eventName("event name")
            .id(1)
            .keyword("keyword")
            .voteNum(2)
            .user(userDto)
            .build();

    when(rsEventRepository.findById(anyInt())).thenReturn(Optional.of(rsEventDto));
    when(userRepository.findById(anyInt())).thenReturn(Optional.of(userDto));
    // when
    rsService.vote(vote, 1);
    // then
    verify(voteRepository)
        .save(
            VoteDto.builder()
                .num(2)
                .localDateTime(localDateTime)
                .user(userDto)
                .rsEvent(rsEventDto)
                .build());
    verify(userRepository).save(userDto);
    verify(rsEventRepository).save(rsEventDto);
  }

  @Test
  void shouldThrowExceptionWhenVoteGivenUserNotExist() {
    // given
    when(rsEventRepository.findById(anyInt())).thenReturn(Optional.empty());
    when(userRepository.findById(anyInt())).thenReturn(Optional.empty());
    //when&then
    assertThrows(
        RuntimeException.class,
        () -> {
          rsService.vote(vote, 1);
        });
  }

  @Test
  void shouldBuyRankSuccess() {
    // Given
    RsEventDto rsEventDto =
            RsEventDto.builder()
                    .eventName("event name")
                    .id(11)
                    .keyword("keyword")
                    .voteNum(2)
                    .isPurchased(0)
                    .build();

    Trade trade = new Trade(BigDecimal.valueOf(10.0), 1, 11);
    when(rsEventRepository.existsById(11)).thenReturn(true);
    when(rsEventRepository.findById(11)).thenReturn(Optional.of(rsEventDto));
    // When
    rsService.buy(trade, 11);

    // Then
    assertEquals(1, rsEventDto.getIsPurchased());

    verify(rsEventRepository).save(rsEventDto);

    verify(tradeRepository).save(TradeDto.builder()
            .amount(BigDecimal.valueOf(10.0))
            .rsEvent(rsEventDto)
            .rank(1)
            .build()
    );

    assertEquals(rsEventDto, rsService.findRsEventByRank(1));
  }

  @Test
  void shouldDeleteRsEventWithSameRank() {
    // Given
    RsEventDto rsEventDto1 =
            RsEventDto.builder()
                    .eventName("event 1")
                    .id(11)
                    .keyword("keyword")
                    .voteNum(3)
                    .isPurchased(0)
                    .build();

    RsEventDto rsEventDto2 =
            RsEventDto.builder()
                    .eventName("event 2")
                    .id(12)
                    .keyword("keyword")
                    .voteNum(2)
                    .isPurchased(0)
                    .build();

    when(rsEventRepository.findUnPurchasedRsEvent(anyObject()))
            .thenReturn(
                    new LinkedList<>(Arrays.asList(rsEventDto1, rsEventDto2)),
                    new LinkedList<>(Arrays.asList(rsEventDto1, rsEventDto2)),
                    new LinkedList<>(),
                    new LinkedList<>());

    when(rsEventRepository.existsById(12)).thenReturn(true);
    when(rsEventRepository.findById(12)).thenReturn(Optional.of(rsEventDto2));

    assertEquals(rsEventDto1, rsService.findRsEventByRank(1));
    assertEquals(rsEventDto2, rsService.findRsEventByRank(2));

    // When
    Trade trade = new Trade(BigDecimal.valueOf(10.0), 1, 12);
    rsService.buy(trade, 12);

    // Then
    assertEquals(1, rsEventDto2.getIsPurchased());

    verify(rsEventRepository).save(rsEventDto2);
    verify(tradeRepository).save(TradeDto.builder()
            .amount(BigDecimal.valueOf(10.0))
            .rsEvent(rsEventDto2)
            .rank(1)
            .build()
    );

    verify(rsEventRepository).deleteById(11);

    assertEquals(rsEventDto2, rsService.findRsEventByRank(1));
    assertEquals(null, rsService.findRsEventByRank(2));
  }

  @Test
  void shouldThrowExceptionWhenBuyRankGivenRankOutOfRange() {
    //Given
    RsEventDto rsEventDto =
            RsEventDto.builder()
                    .eventName("event name")
                    .id(1)
                    .keyword("keyword")
                    .voteNum(2)
                    .isPurchased(0)
                    .build();
    Trade trade1 = new Trade(BigDecimal.valueOf(10.0), 6, 1);
    Trade trade2 = new Trade(BigDecimal.valueOf(10.0), 0, 1);

    when(rsEventRepository.existsById(1)).thenReturn(true);
    when(rsEventRepository.findById(1)).thenReturn(Optional.of(rsEventDto));

    //Given & Then
    assertThrows(RuntimeException.class, ()-> {
      rsService.buy(trade1, 1);
    });

    assertThrows(RuntimeException.class, ()-> {
      rsService.buy(trade2, 1);
    });
  }

  @Test
  void shouldThrowExceptionWhenBuyRankGiveBidNotEnough() {
    //Given
    RsEventDto rsEventDto =
            RsEventDto.builder()
                    .eventName("event name")
                    .id(1)
                    .keyword("keyword")
                    .voteNum(2)
                    .isPurchased(0)
                    .build();
    Trade trade1 = new Trade(BigDecimal.valueOf(10.0), 1, 1);
    Trade trade2 = new Trade(BigDecimal.valueOf(9.9), 1, 1);

    when(rsEventRepository.existsById(1)).thenReturn(true);
    when(rsEventRepository.findById(1)).thenReturn(Optional.of(rsEventDto));

    rsService.buy(trade1, 1);

    //Given & Then

    assertThrows(RuntimeException.class, ()-> {
      rsService.buy(trade2, 1);
    });
  }

  @Test
  void shouldThrowExceptionWhenBuyRankGivenRsEventNotExist() {
    // Given
    Trade trade = new Trade(BigDecimal.valueOf(10.0), 1, 1);

    when(rsEventRepository.existsById(1)).thenReturn(true);
    when(rsEventRepository.findById(1)).thenReturn(Optional.empty());

    // When & Then
    assertThrows(RuntimeException.class, ()-> {
      rsService.buy(trade, 1);
    });
  }
}
