package com.thoughtworks.rslist.dto;

import com.thoughtworks.rslist.domain.RsEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;

@Table(name = "trade")
@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeDto {
    @Id
    @GeneratedValue
    private int id;
    private BigDecimal amount;
    private int rank;
    @ManyToOne
    private RsEventDto rsEvent;
}
