$(function () {
  console.log('hej');
  $('#hc_container').highcharts({
      chart: {
          type: 'column'
      },
      credits: {
        enabled: false
      },
      title: {
          text: 'Performance comparison'
      },
      xAxis: {
          categories: [
              'Apache 2',
              'Wiking',
              'Nginx',
          ],
          labels: {
              rotation: -45,
              align: 'right',
              style: {
                  fontSize: '13px',
                  fontFamily: 'Verdana, sans-serif'
              }
          }
      },
      yAxis: {
          min: 0,
          title: {
              text: 'Requests/second'
          }
      },
      legend: {
          enabled: false
      },
      tooltip: {
          formatter: function() {
              return '<b>'+ this.x +'</b><br/>'+ Highcharts.numberFormat(this.y, 0) +' Requests per second';
          }
      },
      series: [{
          name: 'Performance',
          data: [1969,4846,7492],
          dataLabels: {
              enabled: true,
              style: {
                  fontSize: '13px',
                  fontFamily: 'Verdana, sans-serif'
              }
          }
      }]
  });
});
    
