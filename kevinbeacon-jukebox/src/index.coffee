require ['$api/models', '$views/list#List', '$views/image#Image'], (models, List, Image) ->
  console.log 'spotify loaded...'

  playNextTrack = ->
    $.getJSON 'http://localhost:8080/api/next-track', (track) ->
      models.Track.fromURI(track.uri).load('name', 'artists').done (track) =>
        $('#track-image').attr('src', track.image)
        $('#track-name').text(track.name)
        $('#track-artist').text(track.artists[0].name)
        models.player.playTrack(track)

  models.player.addEventListener 'change', (e) =>
    console.log e.data
    if (e.data.playing is false) and (e.data.track is null)
      console.log 'detected end of track, playing next'
      playNextTrack()

  playNextTrack()
