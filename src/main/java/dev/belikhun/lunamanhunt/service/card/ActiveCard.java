package dev.belikhun.lunamanhunt.service.card;

import dev.belikhun.lunamanhunt.model.card.CardDefinition;
import dev.belikhun.lunamanhunt.model.match.Match;

public record ActiveCard(Match match, CardDefinition card, long appliedAtMillis, long expiresAtMillis) {
}
