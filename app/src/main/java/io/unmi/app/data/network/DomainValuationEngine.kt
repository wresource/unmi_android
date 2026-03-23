package io.unmi.app.data.network

import javax.inject.Inject
import javax.inject.Singleton

data class ValuationResult(
    val estimatedValue: Double,
    val currency: String = "USD",
    val confidence: String, // "high", "medium", "low"
    val factors: Map<String, Double>, // factor name -> score contribution
    val grade: String // S, A, B, C, D, F
)

@Singleton
class DomainValuationEngine @Inject constructor() {

    /**
     * Multi-factor domain valuation algorithm.
     * Factors:
     * 1. Domain length (shorter = more valuable)
     * 2. TLD premium tier
     * 3. Character composition (letters only, no hyphens, no numbers)
     * 4. Dictionary word / pronounceability
     * 5. Renewal cost basis
     * 6. Age (older domains typically worth more)
     * 7. DNSSEC & privacy indicators
     */
    fun evaluate(
        domainName: String,
        tld: String,
        renewPrice: Double = 0.0,
        purchasePrice: Double = 0.0,
        registrationDateStr: String? = null,
        hasWhoisPrivacy: Boolean = false,
        autoRenew: Boolean = false
    ): ValuationResult {
        val sld = domainName.substringBeforeLast(".") // second-level domain
        val factors = mutableMapOf<String, Double>()
        var totalScore = 0.0

        // Factor 1: Length score (max 30 points)
        val lengthScore = when (sld.length) {
            1 -> 30.0
            2 -> 28.0
            3 -> 25.0
            4 -> 20.0
            5 -> 15.0
            6 -> 12.0
            in 7..8 -> 8.0
            in 9..12 -> 4.0
            in 13..20 -> 2.0
            else -> 1.0
        }
        factors["域名长度"] = lengthScore
        totalScore += lengthScore

        // Factor 2: TLD tier (max 25 points)
        val cleanTld = tld.removePrefix(".").lowercase()
        val tldScore = when (cleanTld) {
            "com" -> 25.0
            "org" -> 18.0
            "net" -> 16.0
            "io" -> 15.0
            "ai" -> 20.0
            "dev" -> 12.0
            "co" -> 14.0
            "app" -> 11.0
            "me" -> 10.0
            "cc" -> 9.0
            "tv" -> 9.0
            "cn" -> 12.0
            "com.cn" -> 10.0
            "xyz" -> 5.0
            "top" -> 4.0
            "info" -> 6.0
            "biz" -> 5.0
            "vip" -> 6.0
            "online" -> 4.0
            "site" -> 4.0
            "shop" -> 5.0
            "store" -> 4.0
            "icu" -> 3.0
            else -> 3.0
        }
        factors["后缀价值"] = tldScore
        totalScore += tldScore

        // Factor 3: Character composition (max 20 points)
        val isAllLetters = sld.all { it.isLetter() }
        val isAllDigits = sld.all { it.isDigit() }
        val hasHyphen = sld.contains('-')
        val hasMixedTypes = sld.any { it.isLetter() } && sld.any { it.isDigit() }

        val compositionScore = when {
            isAllLetters && sld.length <= 4 -> 20.0
            isAllDigits && sld.length <= 4 -> 18.0  // Short numeric premium
            isAllLetters -> 15.0
            isAllDigits && sld.length <= 6 -> 14.0
            isAllDigits -> 10.0
            hasHyphen -> 3.0
            hasMixedTypes -> 6.0
            else -> 5.0
        }
        factors["字符组成"] = compositionScore
        totalScore += compositionScore

        // Factor 4: Word quality / pronounceability (max 15 points)
        val wordScore = evaluateWordQuality(sld)
        factors["词汇质量"] = wordScore
        totalScore += wordScore

        // Factor 5: Domain age (max 10 points)
        val ageScore = if (registrationDateStr != null) {
            try {
                val regYear = registrationDateStr.take(4).toInt()
                val currentYear = java.time.LocalDate.now().year
                val age = currentYear - regYear
                when {
                    age >= 20 -> 10.0
                    age >= 15 -> 8.0
                    age >= 10 -> 6.0
                    age >= 5 -> 4.0
                    age >= 2 -> 2.0
                    else -> 1.0
                }
            } catch (e: Exception) { 1.0 }
        } else 0.0
        if (ageScore > 0) factors["域名年龄"] = ageScore
        totalScore += ageScore

        // Convert score to estimated value (USD)
        val estimatedValue = scoreToValue(totalScore, cleanTld, sld.length, renewPrice)

        // Determine confidence
        val confidence = when {
            totalScore >= 70 -> "high"
            totalScore >= 40 -> "medium"
            else -> "low"
        }

        // Grade
        val grade = when {
            totalScore >= 85 -> "S"
            totalScore >= 70 -> "A"
            totalScore >= 55 -> "B"
            totalScore >= 40 -> "C"
            totalScore >= 25 -> "D"
            else -> "F"
        }

        return ValuationResult(
            estimatedValue = estimatedValue,
            currency = "USD",
            confidence = confidence,
            factors = factors,
            grade = grade
        )
    }

    private fun evaluateWordQuality(sld: String): Double {
        val lower = sld.lowercase()

        // Common high-value keywords
        val premiumKeywords = setOf(
            "ai", "app", "web", "pay", "buy", "get", "bet", "car", "tax",
            "job", "sex", "vpn", "api", "crm", "erp", "dns", "cdn", "seo",
            "bank", "cash", "coin", "gold", "loan", "shop", "mall", "deal",
            "game", "play", "tech", "code", "data", "cloud", "cyber", "smart",
            "auto", "home", "life", "love", "food", "wine", "beer", "news",
            "blog", "chat", "mail", "host", "site", "link", "page", "tube"
        )

        // Check if SLD is a known premium keyword
        if (lower in premiumKeywords) return 15.0

        // Check if contains premium keyword
        val containsPremium = premiumKeywords.any { lower.contains(it) && lower.length <= it.length + 4 }
        if (containsPremium) return 10.0

        // Check pronounceability: consonant-vowel patterns
        val vowels = "aeiou"
        val hasVowel = lower.any { it in vowels }
        val hasConsonant = lower.any { it.isLetter() && it !in vowels }
        val vowelRatio = lower.count { it in vowels }.toDouble() / lower.length.coerceAtLeast(1)

        return when {
            !hasVowel -> 3.0 // All consonants (acronym-like)
            !hasConsonant -> 4.0 // All vowels
            vowelRatio in 0.3..0.6 -> 8.0 // Good balance = pronounceable
            vowelRatio in 0.2..0.7 -> 6.0 // Acceptable
            else -> 4.0
        }
    }

    private fun scoreToValue(
        score: Double,
        tld: String,
        length: Int,
        renewPrice: Double
    ): Double {
        // Base value from score using exponential curve
        val baseValue = when {
            score >= 90 -> 50000.0 + (score - 90) * 5000.0
            score >= 80 -> 10000.0 + (score - 80) * 4000.0
            score >= 70 -> 3000.0 + (score - 70) * 700.0
            score >= 60 -> 1000.0 + (score - 60) * 200.0
            score >= 50 -> 300.0 + (score - 50) * 70.0
            score >= 40 -> 100.0 + (score - 40) * 20.0
            score >= 30 -> 30.0 + (score - 30) * 7.0
            score >= 20 -> 15.0 + (score - 20) * 1.5
            else -> 5.0 + score * 0.5
        }

        // Apply TLD multiplier
        val tldMultiplier = when (tld) {
            "com" -> 1.0
            "ai" -> 1.2
            "io" -> 0.6
            "org" -> 0.4
            "net" -> 0.35
            "co" -> 0.35
            "dev" -> 0.3
            "cn" -> 0.25
            "app" -> 0.25
            "me" -> 0.2
            else -> 0.15
        }

        // Length bonus for very short domains
        val lengthMultiplier = when (length) {
            1 -> 10.0
            2 -> 5.0
            3 -> 2.5
            4 -> 1.5
            5 -> 1.0
            else -> 1.0
        }

        val estimated = baseValue * tldMultiplier * lengthMultiplier

        // Floor: at least 2x renewal price
        val floor = renewPrice * 2.0

        return maxOf(estimated, floor).let {
            // Round to reasonable precision
            when {
                it >= 10000 -> Math.round(it / 1000.0) * 1000.0
                it >= 1000 -> Math.round(it / 100.0) * 100.0
                it >= 100 -> Math.round(it / 10.0) * 10.0
                else -> Math.round(it * 100.0) / 100.0
            }
        }
    }
}
