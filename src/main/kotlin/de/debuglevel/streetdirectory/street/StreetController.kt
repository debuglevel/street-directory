package de.debuglevel.streetdirectory.street

import de.debuglevel.streetdirectory.postalcode.PostalcodeService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.tags.Tag
import mu.KotlinLogging
import java.util.*

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/streets")
@Tag(name = "streets")
class StreetController(
    private val postalcodeService: PostalcodeService,
    private val streetService: StreetService,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Get all [Street]s matching the [postalcode] and the [streetname].
     */
    @Get("/{?postalcode,streetname}")
    fun getAllStreets(postalcode: String?, streetname: String?): HttpResponse<List<GetStreetResponse>> {
        logger.debug("Called getAllStreets(postalcode=$postalcode, streetname=$streetname)")

        return try {
            val streets = if (!postalcode.isNullOrBlank()) {
                val postalcodeObj = postalcodeService.get(postalcode)

                if (!streetname.isNullOrBlank()) {
                    listOfNotNull(streetService.get(postalcodeObj.id!!, streetname))
                } else {
                    postalcodeObj.streets
                }
            } else {
                streetService.getAll()
            }

            val getStreetResponses = streets
                .map { GetStreetResponse(it) }

            HttpResponse.ok(getStreetResponses)
        } catch (e: Exception) {
            logger.error(e) { "Unhandled exception" }
            HttpResponse.serverError()
        }
    }

    /**
     * Get a street
     * @param id ID of the street
     * @return A street
     */
    @Get("/{id}")
    fun getOneStreet(id: UUID): HttpResponse<GetStreetResponse> {
        logger.debug("Called getOneStreet($id)")

        return try {
            val street = streetService.get(id)

            val getStreetResponse = GetStreetResponse(street)
            HttpResponse.ok(getStreetResponse)
        } catch (e: StreetService.ItemNotFoundException) {
            logger.debug { "Getting street $id failed: ${e.message}" }
            HttpResponse.notFound()
        } catch (e: Exception) {
            logger.error(e) { "Unhandled exception" }
            HttpResponse.serverError()
        }
    }


    /**
     * Delete a street.
     * @param id ID of the street
     */
    @Delete("/{id}")
    fun deleteOneStreet(id: UUID): HttpResponse<Unit> {
        logger.debug("Called deleteOneStreet($id)")
        return try {
            streetService.delete(id)

            HttpResponse.noContent()
        } catch (e: StreetService.ItemNotFoundException) {
            HttpResponse.notFound()
        } catch (e: Exception) {
            logger.error(e) { "Unhandled exception" }
            HttpResponse.serverError()
        }
    }

    /**
     * Delete all streets.
     */
    @Delete("/")
    fun deleteAllStreets(): HttpResponse<Unit> {
        logger.debug("Called deleteAllStreets()")
        return try {
            streetService.deleteAll()

            HttpResponse.noContent()
        } catch (e: Exception) {
            logger.error(e) { "Unhandled exception" }
            HttpResponse.serverError()
        }
    }

    /**
     * Create a street.
     * This is not meant for productive use, but rather for uploading backups.
     */
    @Post("/")
    fun postOneStreet(addStreetRequest: AddStreetRequest): HttpResponse<AddStreetResponse> {
        logger.debug("Called postOneStreet($addStreetRequest)")

        return try {
            val postalcode = postalcodeService.get(addStreetRequest.postalcode)
            val street = addStreetRequest.toStreet(postalcode)
            val addedStreet = streetService.add(street)

            val addStreetResponse = AddStreetResponse(addedStreet)
            HttpResponse.created(addStreetResponse)
        } catch (e: Exception) {
            logger.error(e) { "Unhandled exception" }
            HttpResponse.serverError()
        }
    }

    //@Post("/populate/{?areaId}") // Does not work, but would be nicer?
    //@Post("/populate{?areaId}") // Does not work, but would be nicer?
    @Post("/populate/{areaId}")
    fun populate(areaId: Long) {
        logger.debug { "Called populate($areaId)" }
        streetService.populate(areaId)
    }
}