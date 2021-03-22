package de.debuglevel.streetlister.overpass

import de.debuglevel.streetlister.postalcode.extraction.overpass.OverpassPostalcodeExtractor
import de.debuglevel.streetlister.postalcode.extraction.overpass.OverpassResultHandler
import de.debuglevel.streetlister.postalcode.extraction.overpass.PostalcodeListHandler
import de.westnordost.osmapi.overpass.OverpassMapDataDao
import mu.KotlinLogging
import java.time.Duration
import kotlin.system.measureTimeMillis

class OverpassQueryExecutor<T>(
    private val overpass: OverpassMapDataDao,
) {
    private val logger = KotlinLogging.logger {}

    fun execute(
        query: String,
        overpassResultHandler: OverpassResultHandler<T>,
        serverTimeout: Duration
    ): List<T> {
        logger.debug { "Executing Overpass query..." }
        logger.trace { "Query:\n$query" }

        val queryDurationMillis = measureTimeMillis {
            overpass.queryTable(query, overpassResultHandler)
        }
        val queryDuration = Duration.ofMillis(queryDurationMillis)
        logger.debug { "Query execution took a round trip time of about $queryDuration" } // includes overhead for transfer, parsing et cetera

        val results = try {
            overpassResultHandler.getResults()
        } catch (e: PostalcodeListHandler.EmptyResultSetException) {
            // if query duration took longer than the server timeout,
            // there is good chance the server timeout was hit
            if (queryDuration >= serverTimeout) {
                throw OverpassPostalcodeExtractor.TimeoutExceededException(serverTimeout, queryDuration)
            } else {
                throw e
            }
        }

        // TODO: possible failures:
        //        - quota reached (what do then?)
        //        - invalid resultset (don't know if and when happens)

        logger.debug { "Executed Overpass query: ${results.count()} results." }
        return results
    }
}