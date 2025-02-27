package org.yardimci.asocialoud.feeds.controller;


import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriUtils;
import org.yardimci.asocialoud.feeds.db.model.Feed;
import org.yardimci.asocialoud.feeds.db.repository.FeedRepository;
import org.yardimci.asocialoud.feeds.dto.FeedDto;
import org.yardimci.asocialoud.feeds.dto.FeedResponseDto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/feeds")
public class FeedController {

    private static final Logger logger = LoggerFactory.getLogger(FeedController.class);

    @Autowired
    private FeedRepository feedRepository;

    //@LoadBalanced
    @GetMapping("/of/{memberId}")
    public FeedResponse findFeeds(@PathVariable("memberId") Long memberId, @RequestParam(value = "start", required = false) Integer start) {
        logger.info("Retrieving all feeds of : {}", memberId);
        FeedResponse feedResponse = new FeedResponse();

        List<Feed> feeds = feedRepository.findAllByMemberIdOrderByPublishDateDesc(memberId, PageRequest.of(start == null ? 0 : start.intValue(), FeedRepository.FETCH_COUNT));
        ModelMapper modelMapper = new ModelMapper();
        List<FeedResponseDto> searchResultList =
                feeds
                        .stream()
                        .map(source -> modelMapper.map(source, FeedResponseDto.class))
                        .collect(Collectors.toList());

        feedResponse.setData(searchResultList);
        feedResponse.setStatus(HttpStatus.OK.toString());
        return feedResponse;
    }


    //@LoadBalanced
    //todo instead of this, ask member service for all followings
    @GetMapping("/followingsof/{memberIds}")
    public FeedResponse findFeedsOfFollowings(@PathVariable("memberIds") Long[] memberIds,
                                              @RequestParam(value = "dateAfter", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date dateAfter,
                                              @RequestParam(value = "start", required = false) Integer start) {
        logger.info("Retrieving all followings feeds");
        FeedResponse feedResponse = new FeedResponse();

        if (memberIds == null || memberIds.length < 1) {
            logger.warn("Missing feed info");
            feedResponse.setStatus(HttpStatus.BAD_REQUEST.toString());
            feedResponse.setData("error.missinginformation");
            return feedResponse;
        }

        PageRequest pageRequest = PageRequest.of(start == null ? 0 : start.intValue(), FeedRepository.FETCH_COUNT);

        List<Feed> feeds = dateAfter == null ? feedRepository.findAllByMemberIdInOrderByPublishDateDesc(memberIds, pageRequest) :
                feedRepository.findAllByMemberIdInAndPublishDateAfterOrderByPublishDateDesc(memberIds, dateAfter, pageRequest);

        ModelMapper modelMapper = new ModelMapper();
        List<FeedResponseDto> searchResultList =
                feeds
                        .stream()
                        .map(source -> modelMapper.map(source, FeedResponseDto.class))
                        .collect(Collectors.toList());

        feedResponse.setData(searchResultList);
        feedResponse.setStatus(HttpStatus.OK.toString());
        return feedResponse;
    }

    // todo check if member exists
    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public FeedResponse create(@RequestBody FeedDto feedDto) {
        logger.info("Save feed request received");
        FeedResponse feedResponse = new FeedResponse();

        if (StringUtils.isEmpty(feedDto.getText()) || feedDto.getMemberId() == null || feedDto.getMemberId() < 1L) {
            logger.warn("Missing feed info");
            feedResponse.setStatus(HttpStatus.BAD_REQUEST.toString());
            feedResponse.setData("error.missinginformation");
            return feedResponse;
        }
        Feed newFeed = new Feed();
        newFeed.setMemberId(feedDto.getMemberId());
        newFeed.setText(HtmlUtils.htmlEscape(feedDto.getText()));
        newFeed.setMediaUri(StringUtils.isEmpty(feedDto.getMediaUri()) ? "" : UriUtils.encodePath(feedDto.getMediaUri() , "UTF-8"));
        newFeed.setPublishDate(new Date());

        try {
            logger.info("Saving feed");
            feedRepository.save(newFeed);
            feedResponse.setStatus(HttpStatus.CREATED.toString());

            ModelMapper modelMapper = new ModelMapper();
            FeedResponseDto feedResponseDto = modelMapper.map(newFeed, FeedResponseDto.class);

            feedResponse.setData(feedResponseDto);
        } catch (Exception e) {
            logger.error("Unable to save feed", e);
            feedResponse.setStatus(HttpStatus.BAD_REQUEST.toString());
            feedResponse.setData("error.servererror");
        }

        return feedResponse;
    }


}
