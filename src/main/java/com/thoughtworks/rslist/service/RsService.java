package com.thoughtworks.rslist.service;

import com.sun.org.apache.regexp.internal.RE;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RsService {
  final RsEventRepository rsEventRepository;
  final UserRepository userRepository;
  final VoteRepository voteRepository;
  final TradeRepository tradeRepository;

  final int MAX_RANK = 5;
  private BigDecimal[] rankPrice;
  private RsEventDto[] rankList = {null, null, null, null, null};

  public RsService(RsEventRepository rsEventRepository, UserRepository userRepository, VoteRepository voteRepository, TradeRepository tradeRepository) {
    this.rsEventRepository = rsEventRepository;
    this.userRepository = userRepository;
    this.voteRepository = voteRepository;
    this.tradeRepository = tradeRepository;

    rankPrice = new BigDecimal[MAX_RANK];
    for(int i = 0; i < MAX_RANK; i++) {
      rankPrice[i] = BigDecimal.valueOf(0.0);
    }
  }

  public void vote(Vote vote, int rsEventId) {
    Optional<RsEventDto> rsEventDto = rsEventRepository.findById(rsEventId);
    Optional<UserDto> userDto = userRepository.findById(vote.getUserId());
    if (!rsEventDto.isPresent()
        || !userDto.isPresent()
        || vote.getVoteNum() > userDto.get().getVoteNum()) {
      throw new RuntimeException();
    }
    VoteDto voteDto =
        VoteDto.builder()
            .localDateTime(vote.getTime())
            .num(vote.getVoteNum())
            .rsEvent(rsEventDto.get())
            .user(userDto.get())
            .build();
    voteRepository.save(voteDto);
    UserDto user = userDto.get();
    user.setVoteNum(user.getVoteNum() - vote.getVoteNum());
    userRepository.save(user);
    RsEventDto rsEvent = rsEventDto.get();
    rsEvent.setVoteNum(rsEvent.getVoteNum() + vote.getVoteNum());
    rsEventRepository.save(rsEvent);
  }

  public void buy(Trade trade, int rsEventId) {
    if(!rsEventRepository.existsById(rsEventId)) {
      throw new RuntimeException();
    }
    if(trade.getAmount().compareTo(rankPrice[trade.getRank()-1]) == -1) {
      throw new RuntimeException();
    }
    RsEventDto eventDto = rsEventRepository.findById(rsEventId).get();

    rankPrice[trade.getRank()-1] = trade.getAmount();
    if(rankList[trade.getRank()-1] != null) {
      rsEventRepository.deleteById(rankList[trade.getRank()-1].getId());
    }

    eventDto.setIsPurchased(1);
    rsEventRepository.save(eventDto);
    if(indexOf(eventDto) != -1) {
      rankList[indexOf(eventDto)] = null;
    }
    rankList[trade.getRank()-1] = eventDto;

    TradeDto tradeDto = TradeDto.builder()
            .amount(trade.getAmount())
            .rank(trade.getRank())
            .rsEvent(eventDto)
            .build();
    tradeRepository.save(tradeDto);
  }

  public RsEventDto findRsEventByRank(int rank) {
      return getRankList()[rank-1];
  }

  private int indexOf(RsEventDto eventDto) {
    int i = 0;
    for(; i < MAX_RANK; ++i) {
      if(rankList[i]!= null && rankList[i].getId() == eventDto.getId()) return i;
    }
    return -1;
  }
  public RsEventDto[] getRankList() {
    int total = 0;
    for(int i = 0; i < MAX_RANK; ++i) {
      if(!isPurchased(i)) total++;
    }
    Pageable pageable = PageRequest.of(1, total, Sort.Direction.DESC, "voteNum");
    List<RsEventDto> events = rsEventRepository.findUnPurchasedRsEvent(pageable);
    for(int i = 0; i < MAX_RANK; ++i) {
      if(!isPurchased(i) && events.size() > 0) {
        rankList[i] = events.get(0);
        events.remove(0);
      }
    }
    return rankList;
  }

  private boolean isPurchased(int rank) {
    return rankPrice[rank] == BigDecimal.valueOf(0.0);
  }
}
